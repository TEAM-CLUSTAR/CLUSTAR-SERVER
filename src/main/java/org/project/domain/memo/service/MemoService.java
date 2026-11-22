package org.project.domain.memo.service;

import org.project.domain.memo.dto.request.MemoAiCreateRequest;
import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.request.MemoRecommendationRequest;
import org.project.domain.memo.dto.request.MemoUpdateRequest;
import org.project.domain.memo.dto.response.*;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoService {

    MemoPresignedUrlResponse issuePresignedUrls(
            Long userId,
            MemoPresignedUrlRequest request
    );

    MemoResponse createMemo(Long userId, MemoCreateRequest request);

    MemoResponse updateMemo(Long userId, Long memoId, MemoUpdateRequest request);

    MemoResponse createAiMemo(Long userId, MemoAiCreateRequest request);

    MemoListDashboardResponse getMemosWithMedia(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorLastViewedAt,
            Long cursorMemoId,
            int size
    );

    MemoDetailResponse getOneMemoDetail(Long userId, Long memoId);

    MemoStructureListResponse getStructureMemo(Long userId);

    void deleteMemo(Long userId, Long memoId);

    MemoListDashboardResponse getAiMemosWithMedia(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorLastViewedAt,
            Long cursorMemoId,
            int size
    );

    MemoSearchResponse searchMemos(Long userId, String query);

    MemoRecentViewedResponse getRecentViewedMemos(Long userId);

    MemoRecommendationResponse recommendMemos(Long userId, MemoRecommendationRequest request);

    // == 휴지통 == //

    MemoTrashListResponse getTrashMemos(Long userId, LocalDateTime cursorDeletedAt, Long cursorMemoId, int size);

    MemoDetailResponse getTrashMemoDetail(Long userId, Long memoId);

    MemoResponse restoreMemo(Long userId, Long memoId);

    void deleteTrashMemoPermanently(Long userId, Long memoId);

    void emptyTrash(Long userId);

    // cutoff 이전에 휴지통으로 옮긴 메모를 batchSize씩 영구 삭제. 반환값 = 삭제한 메모 수
    int purgeExpiredTrash(LocalDateTime cutoff, int batchSize);
}
