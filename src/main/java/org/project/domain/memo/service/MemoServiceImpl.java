package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.dto.request.MemoAiCreateRequest;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.*;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.event.MemoDeletedEvent;
import org.project.domain.memo.event.MemoFileCreatedEvent;
import org.project.domain.memo.event.MemoImageCreatedEvent;
import org.project.domain.memo.event.MemoTextCreatedEvent;
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
import org.project.global.util.FileSizeUtil;
import org.project.global.util.MarkdownUtil;
import org.project.global.util.S3KeyUtil;
import org.project.global.util.S3Util;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

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

    private static final int MAX_IMAGE_COUNT = 5;
    private static final int MAX_FILE_COUNT = 5;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    public MemoPresignedUrlResponse issuePresignedUrls(
            Long userId,
            MemoPresignedUrlRequest request
    ) {
        validateImageCount(request.images());
        validateFileCount(request.files());
        validateBytes(request.images(), MemoPresignedUrlRequest.UploadRequest::bytes, MAX_IMAGE_BYTES, MemoErrorCode.IMAGE_TOO_LARGE);
        validateBytes(request.files(), MemoPresignedUrlRequest.UploadRequest::bytes, MAX_FILE_BYTES, MemoErrorCode.FILE_TOO_LARGE);

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
        User user = getUserOrThrow(userId);

        validateImageCount(request.images());
        validateFileCount(request.files());
        validateBytes(request.images(), MemoCreateRequest.ImageRequest::bytes, MAX_IMAGE_BYTES, MemoErrorCode.IMAGE_TOO_LARGE);
        validateBytes(request.files(), MemoCreateRequest.FileRequest::bytes, MAX_FILE_BYTES, MemoErrorCode.FILE_TOO_LARGE);

        // 메모 생성
        Memo memo = Memo.createMemo(
                request.title(),
                request.content(),
                user
        );

        // 라벨 처리 (중복 제거 + 우선순위)
        attachLabels(memo, request.labelNames(), user);

        // 메모 저장
        Memo savedMemo = memoRepository.save(memo);

        eventPublisher.publishEvent(
                new MemoTextCreatedEvent(
                        savedMemo.getId(),
                        userId
                )
        );

        // 이미지 메타데이터 저장 (optional)
        saveMemoImages(savedMemo, request.images(), userId);

        // 파일 메타데이터 저장 (optional)
        saveMemoFiles(savedMemo, request.files(), userId);

        return MemoResponse.from(savedMemo);
    }

    @Transactional
    @Override
    public MemoResponse createAiMemo(Long userId, MemoAiCreateRequest request) {

        User user = getUserOrThrow(userId);

        List<Long> sourceMemoIds = request.sourceMemoIds().stream()
                .distinct()
                .toList();

        List<Memo> sourceMemos =
                memoRepository.findByIdInWithLabelsAndNotDeleted(userId, sourceMemoIds);

        validateSourceMemos(sourceMemoIds, sourceMemos);

        Memo memo = Memo.createAiMemo(
                request.title(),
                request.content(),
                user,
                sourceMemoIds
        );

        attachLabels(memo, resolveCommonLabelNames(sourceMemos), user);

        Memo savedMemo = memoRepository.save(memo);

        eventPublisher.publishEvent(
                new MemoTextCreatedEvent(
                        savedMemo.getId(),
                        userId
                )
        );

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
                        .map(memo -> mapToDashboardResponse(memo, imageMap, fileMap))
                        .toList();

        return MemoListDashboardResponse.from(responses);

    }

    @Override
    public MemoDetailResponse getOneMemoDetail(Long userId, Long memoId) {

        Memo memo = getMemoOrThrow(memoId);

        // 본인 메모가 아니면 예외
        checkMyMemo(memo, userId);

        // 이미지, 파일정보 매핑
        List<MemoDetailResponse.ImageInfo> images = mapToImageInfos(memo.getMemoImages());
        List<MemoDetailResponse.FileInfo> files = mapToFileInfos(memo.getMemoFiles());

        return MemoDetailResponse.from(memo, images, files);
    }

    @Override
    public MemoStructureListResponse getStructureMemo(Long userId){
        // 메모 + 라벨 한번에 조회
        List<Memo> memos = memoRepository.findAllByUserIdWithLabelsAndNotDeleted(userId);

        List<MemoStructureResponse> responses = memos.stream()
                .map(memo -> MemoStructureResponse.from(memo, MarkdownUtil.strip(memo.getContent())))
                .toList();

        return MemoStructureListResponse.from(responses);
    }

    @Override
    @Transactional
    public void deleteMemo(Long userId, Long memoId) {

        Memo memo = getMemoOrThrow(memoId);

        // 본인 메모가 아니면 예외
        checkMyMemo(memo, userId);

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


    // == 내부 헬퍼 메서드들== //

    // 메모 검증
    private void checkMyMemo(Memo memo, Long userId) {
        if (!memo.getUser().getId().equals(userId)) {
            throw new MemoException(MemoErrorCode.FORBIDDEN_MEMO);
        }
    }

    // User 찾기
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
    }

    // Memo 찾기
    private Memo getMemoOrThrow(Long memoId) {
        return memoRepository.findByIdAndNotDeleted(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
    }

    // 라벨 처리 (중복 제거 + 우선순위)
    private void attachLabels(Memo memo, List<String> labelNames, User user) {
        if (labelNames == null || labelNames.isEmpty()) {
            return;
        }

        List<String> uniqueLabelNames = labelNames.stream()
                .distinct()
                .toList();

        for (int priority = 0; priority < uniqueLabelNames.size(); priority++) {
            String labelName = uniqueLabelNames.get(priority);
            Label label = findOrCreateLabel(labelName, user);
            memo.addLabel(label, priority);
        }
    }

    private Label findOrCreateLabel(String labelName, User user) {
        return labelRepository.findByNameAndUser(labelName, user)
                .orElseGet(() -> labelRepository.save(
                        Label.create(labelName, user)
                ));
    }

    private void validateSourceMemos(List<Long> sourceMemoIds, List<Memo> sourceMemos) {
        Set<Long> requestedIds = Set.copyOf(sourceMemoIds);
        Set<Long> foundIds = sourceMemos.stream()
                .map(Memo::getId)
                .collect(Collectors.toSet());

        if (requestedIds.size() != foundIds.size()) {
            throw new MemoException(MemoErrorCode.SOURCE_MEMO_NOT_FOUND);
        }
    }

    private List<String> resolveCommonLabelNames(List<Memo> sourceMemos) {
        String commonLabel = null;

        for (Memo memo : sourceMemos) {
            List<Label> labels = memo.getLabels();
            if (labels.size() != 1) {
                return List.of();
            }

            String labelName = labels.get(0).getName();
            if (commonLabel == null) {
                commonLabel = labelName;
                continue;
            }

            if (!commonLabel.equals(labelName)) {
                return List.of();
            }
        }

        if (commonLabel == null) {
            return List.of();
        }

        return List.of(commonLabel);
    }

    private void saveMemoImages(Memo memo, List<MemoCreateRequest.ImageRequest> images, Long userId) {
        if (images == null || images.isEmpty()) {
            return;
        }

        List<MemoImage> memoImages = images.stream()
                .map(r -> createMemoImage(memo, r, userId))
                .toList();

        memoImageRepository.saveAll(memoImages);

        eventPublisher.publishEvent(
                new MemoImageCreatedEvent(
                        memo.getId(),
                        userId,
                        memoImages.stream()
                                .map(MemoImage::getId)
                                .toList()
                )
        );
    }

    private MemoImage createMemoImage(Memo memo, MemoCreateRequest.ImageRequest request, Long userId) {
        s3KeyUtil.validateS3KeyOwner(userId, request.s3Key());

        return MemoImage.builder()
                .memo(memo)
                .imageS3Key(request.s3Key())
                .imageName(request.imageName())
                .imageBytes(request.bytes())
                .imageExtension(request.extension())
                .imagePriority(request.priority())
                .build();
    }

    private void saveMemoFiles(Memo memo, List<MemoCreateRequest.FileRequest> files, Long userId) {
        if (files == null || files.isEmpty()) {
            return;
        }

        List<MemoFile> memoFiles = files.stream()
                .map(r -> createMemoFile(memo, r, userId))
                .toList();

        memoFileRepository.saveAll(memoFiles);

        eventPublisher.publishEvent(
                new MemoFileCreatedEvent(
                        memo.getId(),
                        userId,
                        memoFiles.stream()
                                .map(MemoFile::getId)
                                .toList()
                )
        );
    }

    private MemoFile createMemoFile(Memo memo, MemoCreateRequest.FileRequest request, Long userId) {
        s3KeyUtil.validateS3KeyOwner(userId, request.s3Key());

        return MemoFile.builder()
                .memo(memo)
                .fileS3Key(request.s3Key())
                .fileName(request.fileName())
                .fileBytes(request.bytes())
                .fileExtension(request.extension())
                .filePriority(request.priority())
                .build();
    }


    private MemoListDashboardResponse.MemoDashboardResponse mapToDashboardResponse(
            Memo memo,
            Map<Long, List<MemoImage>> imageMap,
            Map<Long, List<MemoFile>> fileMap
    ) {
        List<MemoImage> memoImages = imageMap.getOrDefault(memo.getId(), List.of());
        List<MemoFile> memoFiles = fileMap.getOrDefault(memo.getId(), List.of());

        String representativeImageUrl = findRepresentativeImage(memoImages);

        return MemoListDashboardResponse.MemoDashboardResponse.of(
                memo,
                MarkdownUtil.strip(memo.getContent()),
                representativeImageUrl,
                memoImages.size(),
                memoFiles.size()
        );
    }

    private String findRepresentativeImage(List<MemoImage> images) {
        return images.stream()
                .min(Comparator.comparingInt(MemoImage::getImagePriority))
                .map(img -> s3Util.generatePresignedUrl(img.getImageS3Key()))
                .orElse(null);
    }

    private List<MemoDetailResponse.ImageInfo> mapToImageInfos(List<MemoImage> memoImages) {
        return memoImages.stream()
                .map(image -> new MemoDetailResponse.ImageInfo(
                        image.getId(),
                        s3Util.generatePresignedUrl(image.getImageS3Key()),
                        image.getImageName(),
                        image.getImageExtension(),
                        FileSizeUtil.format(image.getImageBytes())
                ))
                .filter(img -> img.imageUrl() != null)
                .toList();
    }

    private List<MemoDetailResponse.FileInfo> mapToFileInfos(List<MemoFile> memoFiles) {
        return memoFiles.stream()
                .map(file -> new MemoDetailResponse.FileInfo(
                        file.getId(),
                        s3Util.generatePresignedUrl(file.getFileS3Key()),
                        file.getFileName(),
                        file.getFileExtension(),
                        FileSizeUtil.format(file.getFileBytes())
                ))
                .filter(f -> f.fileUrl() != null)
                .toList();
    }

    private void validateImageCount(List<?> images) {
        if (images != null && images.size() > MAX_IMAGE_COUNT) {
            throw new MemoException(MemoErrorCode.TOO_MANY_IMAGES);
        }
    }

    private void validateFileCount(List<?> files) {
        if (files != null && files.size() > MAX_FILE_COUNT) {
            throw new MemoException(MemoErrorCode.TOO_MANY_FILES);
        }
    }

    private <T> void validateBytes(
            List<T> items,
            Function<T, Long> sizeExtractor,
            long maxBytes,
            MemoErrorCode errorCode
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }

        boolean anyTooLarge = items.stream()
                .map(sizeExtractor)
                .anyMatch(size -> size != null && size > maxBytes);

        if (anyTooLarge) {
            throw new MemoException(errorCode);
        }
    }

}
