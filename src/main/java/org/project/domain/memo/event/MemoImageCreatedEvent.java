package org.project.domain.memo.event;

import java.util.List;

public record MemoImageCreatedEvent(
        Long memoId,
        Long userId,
        List<Long> memoImageIds
) {
}

