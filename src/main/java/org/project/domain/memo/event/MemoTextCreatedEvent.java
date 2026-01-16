package org.project.domain.memo.event;

public record MemoTextCreatedEvent(
        Long memoId,
        Long userId
) {
}
