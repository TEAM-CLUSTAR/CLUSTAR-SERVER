package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.UserErrorCode;
import org.project.global.util.S3Util;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoServiceImpl implements MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final S3Util s3Util;

    @Transactional
    @Override
    public MemoResponse createMemo(Long userId, MemoCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        Memo memo = Memo.createMemo(request.title(), request.content(), user);

        if (request.labelNames() != null && !request.labelNames().isEmpty()) {
            List<String> uniqueLabelNames = request.labelNames().stream()
                    .distinct()
                    .toList();

            for (int i = 0; i < uniqueLabelNames.size(); i++) {
                String labelName = uniqueLabelNames.get(i);

                // 해당 유저의 라벨 찾기 (없으면 생성)
                Label label = labelRepository.findByNameAndUser(labelName, user)
                        .orElseGet(() -> labelRepository.save(
                                Label.create(labelName, user)
                        ));

                // i가 우선순위임 (0, 1, 2, ...)
                memo.addLabel(label, i);
            }
        }

        Memo savedMemo = memoRepository.save(memo);

        return MemoResponse.from(savedMemo);
    }

    @Override
    public MemoListDashboardResponse getMemos(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    ) {

        List<Memo> memos = memoRepository.findMemos(
                userId,
                labelIds,
                cursorCreatedAt,
                cursorMemoId,
                PageRequest.of(0, size)
        );

        return MemoListDashboardResponse.from(memos);
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

        memo.getMemoImages().forEach(memoImage -> {
            s3Util.deleteFile(memoImage.getImageS3Key());
        });

        memo.getMemoFiles().forEach(memoFile -> {
            s3Util.deleteFile(memoFile.getFileS3Key());
        });

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
