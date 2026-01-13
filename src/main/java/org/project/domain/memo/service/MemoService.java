package org.project.domain.memo.service;

import org.project.domain.memo.dto.request.MemoCreateRequest;
import org.project.domain.memo.dto.response.MemoDetailResponse;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;
import org.project.domain.memo.dto.response.MemoResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoService {

    MemoResponse createMemo(Long userId, MemoCreateRequest request);

    MemoListDashboardResponse getMemos(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            int size
    );

    MemoDetailResponse getOneMemoDetail(Long userId, Long memoId);

    void deleteMemo(Long userId, Long memoId);
}
