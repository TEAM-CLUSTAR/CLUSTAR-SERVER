package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.event.MemoDeletedEvent;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.domain.memo.repository.MemoLabelRepository;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.UserErrorCode;
import org.project.global.util.S3KeyUtil;
import org.project.global.util.S3Util;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoServiceImpl implements MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;

    private final MemoImageRepository memoImageRepository;
    private final MemoFileRepository memoFileRepository;
    private final MemoLabelRepository memoLabelRepository;

    private final S3KeyUtil s3KeyUtil;
    private final S3Util s3Util;

    private final ApplicationEventPublisher eventPublisher;

    public MemoPresignedUrlResponse issuePresignedUrls(
            Long userId,
            MemoPresignedUrlRequest request
    ) {

        List<MemoPresignedUrlResponse.PresignedUrlResponse> imageUrls =
                request.images().stream()
                        .map(r -> s3Util.createPresignedPutUrl(
                                userId,
                                "memo-image",
                                r.extension(),
                                r.bytes(),
                                r.priority()
                        ))
                        .toList();

        List<MemoPresignedUrlResponse.PresignedUrlResponse> fileUrls =
                request.files().stream()
                        .map(r -> s3Util.createPresignedPutUrl(
                                userId,
                                "memo-file",
                                r.extension(),
                                r.bytes(),
                                r.priority()
                        ))
                        .toList();

        return new MemoPresignedUrlResponse(imageUrls, fileUrls);
    }

    @Transactional
    @Override
    public MemoResponse createMemo(Long userId, MemoCreateRequest request) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        // 메모 생성
        Memo memo = Memo.createMemo(
                request.title(),
                request.content(),
                user
        );

        // 라벨 처리 (중복 제거 + 우선순위)
        if (request.labelNames() != null && !request.labelNames().isEmpty()) {

            List<String> uniqueLabelNames = request.labelNames().stream()
                    .distinct()
                    .toList();

            for (int priority = 0; priority < uniqueLabelNames.size(); priority++) {
                String labelName = uniqueLabelNames.get(priority);

                Label label = labelRepository.findByNameAndUser(labelName, user)
                        .orElseGet(() ->
                                labelRepository.save(
                                        Label.create(labelName, user)
                                )
                        );

                memo.addLabel(label, priority);
            }
        }

        // 메모 저장
        Memo savedMemo = memoRepository.save(memo);

        // 이미지 메타데이터 저장 (optional)
        if (request.images() != null && !request.images().isEmpty()) {

            List<MemoImage> images = request.images().stream()
                    .map(r -> {
                        s3KeyUtil.validateS3KeyOwner(userId, r.s3Key());

                        return MemoImage.builder()
                                .memo(savedMemo)
                                .imageS3Key(r.s3Key())
                                .imageBytes(r.bytes())
                                .imageExtension(r.extension())
                                .imagePriority(r.priority())
                                .build();
                    })
                    .toList();

            memoImageRepository.saveAll(images);
        }

        // 파일 메타데이터 저장 (optional)
        if (request.files() != null && !request.files().isEmpty()) {

            List<MemoFile> files = request.files().stream()
                    .map(r -> {
                        s3KeyUtil.validateS3KeyOwner(userId, r.s3Key());

                        return MemoFile.builder()
                                .memo(savedMemo)
                                .fileS3Key(r.s3Key())
                                .fileBytes(r.bytes())
                                .fileExtension(r.extension())
                                .filePriority(r.priority())
                                .build();
                    })
                    .toList();

            memoFileRepository.saveAll(files);
        }

        return MemoResponse.from(savedMemo);
    }

    @Transactional(readOnly = true)
    @Override
    public MemoListDashboardResponse getMemosWithMedia(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    ) {

        // 메모 조회 (기존 로직 재사용)
        List<Memo> memos = memoRepository.findMemos(
                userId,
                labelIds,
                cursorCreatedAt,
                cursorMemoId,
                PageRequest.of(0, size)
        );

        if (memos.isEmpty()) {
            return MemoListDashboardResponse.from(List.of());
        }

        // memoId 목록 추출
        List<Long> memoIds = memos.stream()
                .map(Memo::getId)
                .toList();

        // 이미지 / 파일 조회
        List<MemoImage> images = memoImageRepository.findByMemoIdIn(memoIds);
        List<MemoFile> files = memoFileRepository.findByMemoIdIn(memoIds);

        // memoId 기준 그룹핑
        Map<Long, List<MemoImage>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getMemo().getId()
                ));

        Map<Long, List<MemoFile>> fileMap = files.stream()
                .collect(Collectors.groupingBy(
                        file -> file.getMemo().getId()
                ));

        // 응답 조립
        List<MemoListDashboardResponse.MemoDashboardResponse> responses =
                memos.stream()
                        .map(memo -> {

                            List<MemoImage> memoImages =
                                    imageMap.getOrDefault(memo.getId(), List.of());

                            List<MemoFile> memoFiles =
                                    fileMap.getOrDefault(memo.getId(), List.of());

                            // 대표 이미지 (priority ASC)
                            String representativeImageUrl = memoImages.stream()
                                    .min(Comparator.comparingInt(MemoImage::getImagePriority))
                                    .map(img -> s3Util.generatePresignedUrl(img.getImageS3Key()))
                                    .orElse(null);

                            return MemoListDashboardResponse.MemoDashboardResponse.of(
                                    memo,
                                    representativeImageUrl,
                                    memoImages.size(),
                                    memoFiles.size()
                            );
                        })
                        .toList();

        return MemoListDashboardResponse.from(responses);
    }

    @Override
    public MemoDetailResponse getOneMemoDetail(Long userId, Long memoId) {

        Memo memo = memoRepository.findByIdAndNotDeleted(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));

        // 본인 메모인지 확인
        if (!memo.getUser().getId().equals(userId)) {
            throw new MemoException(MemoErrorCode.FORBIDDEN_MEMO);
        }

        List<String> imageUrls = memo.getMemoImages().stream()
                .map(memoImage -> s3Util.generatePresignedUrl(memoImage.getImageS3Key()))
                .filter(url -> url != null)
                .toList();

        List<MemoDetailResponse.FileInfo> files = memo.getMemoFiles().stream()
                .map(memoFile -> new MemoDetailResponse.FileInfo(
                        memoFile.getId(),
                        s3Util.generatePresignedUrl(memoFile.getFileS3Key()),
                        memoFile.getFileExtension(),
                        memoFile.getFileBytes()
                ))
                .filter(file -> file.fileUrl() != null)
                .toList();

        return MemoDetailResponse.from(memo, imageUrls, files);
    }

    @Override
    @Transactional
    public void deleteMemo(Long userId, Long memoId) {

        Memo memo = memoRepository.findByIdAndNotDeleted(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));

        // 본인 메모인지 확인
        if (!memo.getUser().getId().equals(userId)) {
            throw new MemoException(MemoErrorCode.FORBIDDEN_MEMO);
        }

        // 삭제할 S3 키 수집
        List<String> imageKeys = memo.getMemoImages().stream()
                        .map(MemoImage::getImageS3Key)
                                .toList();

        List<String> fileKeys = memo.getMemoFiles().stream()
                        .map(MemoFile::getFileS3Key)
                                .toList();

        // DB 삭제 hard/soft delete
        memoImageRepository.deleteByMemo(memo);
        memoFileRepository.deleteByMemo(memo);
        memoLabelRepository.deleteByMemo(memo);
        memo.delete();

        // 이벤트 발행(트랜잭션 커밋 후 실행됨)
        eventPublisher.publishEvent(new MemoDeletedEvent(memoId, imageKeys, fileKeys));
    }

}
