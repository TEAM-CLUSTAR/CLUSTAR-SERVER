package org.project.domain.memo.service;

import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.request.MemoAiCreateRequest;
import org.project.domain.memo.dto.request.MemoPresignedUrlRequest;
import org.project.domain.memo.dto.response.*;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoService {

    MemoPresignedUrlResponse issuePresignedUrls(
            Long userId,
            MemoPresignedUrlRequest request
    );

    MemoResponse createMemo(Long userId, MemoCreateRequest request);

    MemoResponse createAiMemo(Long userId, MemoAiCreateRequest request);

    MemoListDashboardResponse getMemosWithMedia(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    );

    MemoDetailResponse getOneMemoDetail(Long userId, Long memoId);

    MemoStructureListResponse getStructureMemo(Long userId);

    void deleteMemo(Long userId, Long memoId);

    MemoListDashboardResponse getAiMemosWithMedia(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    );
}
