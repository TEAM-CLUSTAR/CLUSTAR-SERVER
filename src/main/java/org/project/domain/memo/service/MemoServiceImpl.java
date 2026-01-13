package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
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
import org.project.global.util.S3PresignedUtil;
import org.project.global.util.S3KeyUtil;
import org.project.global.util.S3Util;
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

    private final S3PresignedUtil s3PresignedUtil;
    private final S3KeyUtil s3KeyUtil;

    private final MemoImageRepository memoImageRepository;
    private final MemoFileRepository memoFileRepository;
    private final MemoLabelRepository memoLabelRepository;

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
                                    .map(img -> s3PresignedUtil.generateGetUrl(img.getImageS3Key()))
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

        // S3 파일들 삭제
        memo.getMemoImages().forEach(memoImage -> {
            s3Util.deleteFile(memoImage.getImageS3Key());
        });

        memo.getMemoFiles().forEach(memoFile -> {
            s3Util.deleteFile(memoFile.getFileS3Key());
        });

        // 연관 엔티티들 hard delete
        memoImageRepository.deleteByMemo(memo);
        memoFileRepository.deleteByMemo(memo);
        memoLabelRepository.deleteByMemo(memo);

        memo.delete();
    }

    // == 메모 생성을 위한 임시 테스트 메서드들이므로 추후 삭제 == //
    @Transactional
    public MemoResponse createMemoWithFiles(
            Long userId,
            String title,
            String content,
            List<String> labelNames,
            List<MultipartFile> images,
            List<MultipartFile> files
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        // 메모 생성
        Memo memo = Memo.createMemo(title, content, user);

        // 라벨 추가
        if (labelNames != null && !labelNames.isEmpty()) {
            List<String> uniqueLabelNames = labelNames.stream()
                    .distinct()
                    .toList();

            for (int i = 0; i < uniqueLabelNames.size(); i++) {
                String labelName = uniqueLabelNames.get(i);
                Label label = labelRepository.findByNameAndUser(labelName, user)
                        .orElseGet(() -> labelRepository.save(
                                Label.create(labelName, user)
                        ));
                memo.addLabel(label, i);
            }
        }

        Memo savedMemo = memoRepository.save(memo);

        // 이미지 업로드
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                if (!image.isEmpty()) {
                    String imageKey = s3Util.uploadFile(image, "memo-image", userId);
                    String extension = getFileExtension(image.getOriginalFilename());

                    savedMemo.addImage(
                            imageKey,
                            image.getSize(),
                            extension,
                            i
                    );
                }
            }
        }

        // 파일 업로드
        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (!file.isEmpty()) {
                    String fileKey = s3Util.uploadFile(file, "memo-file", userId);
                    String extension = getFileExtension(file.getOriginalFilename());

                    savedMemo.addFile(
                            fileKey,
                            file.getSize(),
                            extension,
                            i
                    );
                }
            }
        }

        return MemoResponse.from(savedMemo);
    }

    // 추가: 확장자 추출 헬퍼 메서드
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

}
