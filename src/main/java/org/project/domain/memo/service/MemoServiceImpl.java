package org.project.domain.memo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.tag.entity.Tag;
import org.project.domain.tag.repository.TagRepository;
import org.project.domain.memo.config.MemoRecommendationProperties;
import org.project.domain.memo.config.MemoSearchProperties;
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
import org.project.domain.memo.repository.MemoTagRepository;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.ai.rag.E.retrieve.search.MemoSearchVectorRetriever;
import org.project.domain.memo.dto.request.MemoRecommendationRequest;
import org.project.domain.memo.dto.response.MemoRecommendationItemResponse;
import org.project.domain.memo.dto.response.MemoRecommendationResponse;
import org.project.domain.memo.repository.VectorStoreRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoServiceImpl implements MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    private final MemoImageRepository memoImageRepository;
    private final MemoFileRepository memoFileRepository;
    private final MemoTagRepository memoTagRepository;

    private final S3KeyUtil s3KeyUtil;
    private final S3Util s3Util;

    private final ApplicationEventPublisher eventPublisher;
    private final MemoSearchVectorRetriever memoSearchVectorRetriever;
    private final VectorStoreRepository vectorStoreRepository;
    private final MemoRecommendationProperties memoRecommendationProperties;
    private final MemoSearchProperties memoSearchProperties;

    @Autowired
    @Qualifier("ioExecutor")
    private Executor ioExecutor;

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

        // 태그 처리 (중복 제거 + 우선순위)
        attachTags(memo, request.tagNames(), user);

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
                memoRepository.findByIdInWithTagsAndNotDeleted(userId, sourceMemoIds);

        validateSourceMemos(sourceMemoIds, sourceMemos);

        Memo memo = Memo.createAiMemo(
                request.title(),
                request.content(),
                user,
                sourceMemoIds
        );

        attachTags(memo, resolveCommonTagNames(sourceMemos), user);

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
            List<Long> tagIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    ) {

        long totalCount;

        if(tagIds == null || tagIds.isEmpty()) {
            totalCount = memoRepository.countAllMemos(userId);
        } else {
            totalCount = memoRepository.countMemosByTags(userId, tagIds);
        }

        // 메모 조회 (기존 로직 재사용)
        List<Memo> memos = memoRepository.findMemos(
                userId,
                tagIds,
                cursorCreatedAt,
                cursorMemoId,
                PageRequest.of(0, size)
        );

        if (memos.isEmpty()) {
            return MemoListDashboardResponse.from(totalCount, List.of());
        }

        // memoId 목록 추출
        List<Long> memoIds = memos.stream()
                .map(Memo::getId)
                .toList();

        // 이미지 / 파일: 대표 이미지 s3Key + 개수만 조회 (전체 엔티티 조회 대신 프로젝션)
        Map<Long, String> representativeImageS3KeyMap = memoImageRepository.findRepresentativeImageS3Keys(memoIds);
        Map<Long, Long> imageCountMap = memoImageRepository.countImagesByMemoId(memoIds);
        Map<Long, Long> fileCountMap = memoFileRepository.countFilesByMemoId(memoIds);

        // 응답 조립
        List<MemoListDashboardResponse.MemoDashboardResponse> responses =
                memos.stream()
                        .map(memo -> mapToDashboardResponse(memo, representativeImageS3KeyMap, imageCountMap, fileCountMap))
                        .toList();

        return MemoListDashboardResponse.from(totalCount, responses);
    }

    @Transactional(readOnly = true)
    @Override
    public MemoListDashboardResponse getAiMemosWithMedia(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    ) {

        long totalCount;

        if(tagIds == null || tagIds.isEmpty()) {
            totalCount = memoRepository.countAllMemos(userId);
        } else {
            totalCount = memoRepository.countMemosByTags(userId, tagIds);
        }

        // 메모 조회 (기존 로직 재사용)
        List<Memo> memos = memoRepository.findAiMemos(
                userId,
                tagIds,
                cursorCreatedAt,
                cursorMemoId,
                PageRequest.of(0, size)
        );

        if (memos.isEmpty()) {
            return MemoListDashboardResponse.from(totalCount, List.of());
        }

        // memoId 목록 추출
        List<Long> memoIds = memos.stream()
                .map(Memo::getId)
                .toList();

        // 이미지 / 파일: 대표 이미지 s3Key + 개수만 조회 (전체 엔티티 조회 대신 프로젝션)
        Map<Long, String> representativeImageS3KeyMap = memoImageRepository.findRepresentativeImageS3Keys(memoIds);
        Map<Long, Long> imageCountMap = memoImageRepository.countImagesByMemoId(memoIds);
        Map<Long, Long> fileCountMap = memoFileRepository.countFilesByMemoId(memoIds);

        // 응답 조립
        List<MemoListDashboardResponse.MemoDashboardResponse> responses =
                memos.stream()
                        .map(memo -> mapToDashboardResponse(memo, representativeImageS3KeyMap, imageCountMap, fileCountMap))
                        .toList();

        return MemoListDashboardResponse.from(totalCount, responses);
    }

    @Override
    @Transactional
    public MemoDetailResponse getOneMemoDetail(Long userId, Long memoId) {

        Memo memo = getMemoOrThrow(memoId);

        // 본인 메모가 아니면 예외
        checkMyMemo(memo, userId);

        // 이미지, 파일정보 매핑
        List<MemoDetailResponse.ImageInfo> images = mapToImageInfos(memo.getMemoImages());
        List<MemoDetailResponse.FileInfo> files = mapToFileInfos(memo.getMemoFiles());

        List<Memo> sourceMemos = Collections.emptyList();

        if (Boolean.TRUE.equals(memo.getIsAiGenerated())
                && memo.getSource() != null
                && !memo.getSource().isBlank()) {

            List<Long> sourceIds = parseSourceIds(memo.getSource());
            sourceMemos = memoRepository.findAllById(sourceIds);
        }

        memo.markAsRead();
        // 상세조회 = 열람으로 간주해 열람 시각 갱신 (최근 열람 목록/카드 날짜에 사용)
        memo.markViewed();

        return MemoDetailResponse.from(
                memo,
                images,
                files,
                sourceMemos
        );
    }

    @Override
    public MemoStructureListResponse getStructureMemo(Long userId){
        // 메모 + 태그 한번에 조회
        List<Memo> memos = memoRepository.findAllByUserIdWithTagsAndNotDeleted(userId);

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
        memoTagRepository.deleteByMemo(memo);
        memo.delete();

        // 이벤트 발행(트랜잭션 커밋 후 실행됨)
        eventPublisher.publishEvent(new MemoDeletedEvent(memoId, imageKeys, fileKeys));
    }


    @Override
    public MemoSearchResponse searchMemos(Long userId, String query) {
        if (query == null || query.isBlank()) {
            throw new MemoException(MemoErrorCode.EMPTY_SEARCH_QUERY);
        }

        // 키워드(텍스트) 기반 검색: 검색어가 제목/태그/본문에 포함된 모든 메모를
        // 필드 우선순위(제목>태그>본문) + 온전한 키워드 매칭 여부 + 최신순으로 정렬해 반환한다.
        List<Memo> textResults = memoRepository.searchByText(userId, query);

        List<MemoSearchItemResponse> results = textResults.stream()
                .map(memo -> MemoSearchItemResponse.from(memo, SearchType.TEXT))
                .toList();

        // [보존] 추후 이용될 수도 있으므로 의미 기반 검색 보존
        /*
         * // 텍스트 검색 (병렬) — 실패해도 의미 검색 결과라도 주도록 빈 리스트로 degrade
         * CompletableFuture<List<Memo>> textFuture = CompletableFuture.supplyAsync(
         *         () -> {
         *             try {
         *                 return memoRepository.searchByText(userId, query); // (구: searchByText(userId, query, 3))
         *             } catch (Exception e) {
         *                 log.warn("[Search] 텍스트 검색 실패, 의미 검색 결과만 반환합니다. userId={} errorType={}",
         *                         userId, e.getClass().getSimpleName());
         *                 return List.<Memo>of();
         *             }
         *         }, ioExecutor
         * );
         *
         * // 의미 기반 벡터 검색 (병렬)
         * CompletableFuture<List<Memo>> vectorFuture = CompletableFuture.supplyAsync(
         *         () -> memoSearchVectorRetriever.retrieve(userId, query), ioExecutor
         * );
         *
         * CompletableFuture.allOf(textFuture, vectorFuture).join();
         *
         * List<Memo> textResults = textFuture.join();
         * List<Memo> vectorResults = vectorFuture.join();
         *
         * // 텍스트 결과 먼저, 중복 memoId 제거하며 벡터 결과 추가
         * Set<Long> seenIds = new LinkedHashSet<>();
         * List<MemoSearchItemResponse> results = new ArrayList<>();
         *
         * textResults.forEach(memo -> {
         *     if (seenIds.add(memo.getId())) {
         *         results.add(MemoSearchItemResponse.from(memo, SearchType.TEXT));
         *     }
         * });
         *
         * // 벡터 결과는 텍스트와 중복 제거를 먼저 한 뒤 상위 maxMemoResults개만 가져가게
         * int maxSemantic = memoSearchProperties.getMaxMemoResults();
         * int semanticAdded = 0;
         * for (Memo memo : vectorResults) {
         *     if (semanticAdded >= maxSemantic) {
         *         break;
         *     }
         *     if (seenIds.add(memo.getId())) {
         *         results.add(MemoSearchItemResponse.from(memo, SearchType.SEMANTIC));
         *         semanticAdded++;
         *     }
         * }
         *
         * String message = vectorResults.isEmpty() ? "의미상 유사한 메모를 찾지 못했어요." : null;
         * return MemoSearchResponse.of(results, message);
         */

        String message = results.isEmpty() ? "검색 결과가 없어요." : null;
        return MemoSearchResponse.of(results, message);
    }

    @Override
    public MemoRecentViewedResponse getRecentViewedMemos(Long userId) {
        // 검색 모달 [입력 완료 전] — 최근 열람한 메모를 최신 열람순으로 상위 N개 반환.
        int limit = memoSearchProperties.getRecentViewedLimit();
        List<Memo> memos = memoRepository.findRecentViewed(userId, limit);

        // 열람 이력이 하나도 없으면 검색 진입 화면이 비지 않도록 최근 생성 메모로 폴백한다.
        RecentViewedSource source = RecentViewedSource.RECENT_VIEWED;
        if (memos.isEmpty()) {
            source = RecentViewedSource.RECENT_CREATED;
            memos = memoRepository.findRecentCreated(userId, limit);
        }

        List<MemoRecentViewedResponse.Item> items = memos.stream()
                .map(MemoRecentViewedResponse.Item::from)
                .toList();

        return MemoRecentViewedResponse.of(source, items);
    }

    @Override
    public MemoRecommendationResponse recommendMemos(Long userId, MemoRecommendationRequest request) {
        // 선택한 메모가 모두 본인 소유이며 삭제되지 않았는지 검증 (남의 메모/없는 메모로 추천 요청 방지)
        List<Long> selectedMemoIds = request.memoIds();
        long ownedCount = memoRepository.countByIdInAndUserIdAndNotDeleted(userId, selectedMemoIds);
        if (ownedCount != selectedMemoIds.stream().distinct().count()) {
            throw new MemoException(MemoErrorCode.SOURCE_MEMO_NOT_FOUND);
        }

        // Gate 1: 선택된 메모들 자체가 서로 응집돼 있지 않으면(평균 벡터가 대표성이 없으면) 후보 검색 없이 바로 종료
        Double cohesion = vectorStoreRepository.computeSelectionCohesion(userId, request.memoIds());
        if (cohesion != null && cohesion < memoRecommendationProperties.getCohesionThreshold()) {
            return MemoRecommendationResponse.empty("선택한 메모들끼리 연관성이 없어요.");
        }

        // Gate 2: 응집된 선택 기준으로 유사 후보 검색
        // 단일 선택은 평균화 효과가 없어 벡터 간 유사도가 구조적으로 낮게 나오므로 별도 임계값을 쓴다
        double candidateThreshold = request.memoIds().size() == 1
                ? memoRecommendationProperties.getSingleSelectionSimilarityThreshold()
                : memoRecommendationProperties.getCandidateSimilarityThreshold();
        List<Long> recommendedIds = vectorStoreRepository.findRecommendedMemoIds(userId, request.memoIds(), candidateThreshold);

        if (recommendedIds.isEmpty()) {
            return MemoRecommendationResponse.of(List.of());
        }

        List<Long> top3Ids = recommendedIds.stream().limit(3).toList();

        List<Memo> memos = memoRepository.findByIdInWithTagsAndNotDeleted(userId, top3Ids);

        Map<Long, Memo> memoById = memos.stream()
                .collect(Collectors.toMap(Memo::getId, Function.identity()));

        List<MemoRecommendationItemResponse> items = top3Ids.stream()
                .map(memoById::get)
                .filter(Objects::nonNull)
                .map(MemoRecommendationItemResponse::from)
                .toList();

        return MemoRecommendationResponse.of(items);
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

    // 태그 처리 (중복 제거 + 우선순위) — 이름 목록으로 기존 태그 일괄 조회 후, 없는 것만 일괄 생성
    private void attachTags(Memo memo, List<String> tagNames, User user) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        List<String> uniqueTagNames = tagNames.stream()
                .distinct()
                .toList();

        Map<String, Tag> tagsByName = findOrCreateTags(uniqueTagNames, user);

        for (int priority = 0; priority < uniqueTagNames.size(); priority++) {
            String tagName = uniqueTagNames.get(priority);
            memo.addTag(tagsByName.get(tagName), priority);
        }
    }

    private Map<String, Tag> findOrCreateTags(List<String> tagNames, User user) {
        List<Tag> existingTags = tagRepository.findAllByNameInAndUser(tagNames, user);
        Map<String, Tag> tagsByName = new HashMap<>(existingTags.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity())));

        List<Tag> newTags = tagNames.stream()
                .filter(name -> !tagsByName.containsKey(name))
                .map(name -> Tag.create(name, user))
                .toList();

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags)
                    .forEach(tag -> tagsByName.put(tag.getName(), tag));
        }

        return tagsByName;
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

    private List<String> resolveCommonTagNames(List<Memo> sourceMemos) {
        String commonTag = null;

        for (Memo memo : sourceMemos) {
            List<Tag> tags = memo.getTags();
            if (tags.size() != 1) {
                return List.of();
            }

            String tagName = tags.get(0).getName();
            if (commonTag == null) {
                commonTag = tagName;
                continue;
            }

            if (!commonTag.equals(tagName)) {
                return List.of();
            }
        }

        if (commonTag == null) {
            return List.of();
        }

        return List.of(commonTag);
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
            Map<Long, String> representativeImageS3KeyMap,
            Map<Long, Long> imageCountMap,
            Map<Long, Long> fileCountMap
    ) {
        String s3Key = representativeImageS3KeyMap.get(memo.getId());
        String representativeImageUrl = (s3Key != null) ? s3Util.generatePresignedUrl(s3Key) : null;

        return MemoListDashboardResponse.MemoDashboardResponse.of(
                memo,
                MarkdownUtil.strip(memo.getContent()),
                representativeImageUrl,
                imageCountMap.getOrDefault(memo.getId(), 0L).intValue(),
                fileCountMap.getOrDefault(memo.getId(), 0L).intValue()
        );
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

    private List<Long> parseSourceIds(String source) {
        if (source == null || source.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = source.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (normalized.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return Arrays.stream(normalized.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }
}
