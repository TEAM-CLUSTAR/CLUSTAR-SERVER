package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoRepositoryCustom {
    List<Memo> findMemos(
            Long userId,
            List<Long> labelIds,
            LocalDateTime cursorCreatedAt,
            Long cursorMemoId,
            Pageable pageable
    );
}
