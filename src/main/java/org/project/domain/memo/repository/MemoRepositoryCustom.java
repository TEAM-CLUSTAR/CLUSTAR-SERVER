package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoRepositoryCustom {
    List<Memo> findMemos(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorLastViewedAt,
            Long cursorMemoId,
            Pageable pageable
    );

    List<Memo> findAiMemos(
            Long userId,
            List<Long> tagIds,
            LocalDateTime cursorLastViewedAt,
            Long cursorMemoId,
            Pageable pageable
    );

    List<Memo> findAllByUserIdWithTagsOrderByLastViewedAt(Long userId);

    List<Memo> searchByText(Long userId, String query);

    // 작성일 [from, to) 범위의 메모만 텍스트 검색 (null이면 해당 방향 제한 없음)
    List<Memo> searchByText(Long userId, String query, LocalDateTime from, LocalDateTime to);

    // 작성일 [from, to) 범위의 메모 id
    List<Long> findIdsByCreatedAtRange(Long userId, LocalDateTime from, LocalDateTime to);

    // 작성일 [from, to) 범위의 메모를 작성일순으로 최대 limit개 조회 (시간 단서만 있는 검색용)
    List<Memo> findByCreatedAtRange(Long userId, LocalDateTime from, LocalDateTime to, boolean ascending, int limit);

    List<Memo> findRecentViewed(Long userId, int limit);

    List<Memo> findRecentCreated(Long userId, int limit);
}
