package org.project.domain.memo.event;

import java.util.List;

public record MemoFileCreatedEvent(
        Long memoId,
        Long userId,
        List<Long> memoFileIds
) {
}
