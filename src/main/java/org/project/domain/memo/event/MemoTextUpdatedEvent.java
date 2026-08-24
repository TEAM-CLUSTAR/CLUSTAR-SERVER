package org.project.domain.memo.event;

// 제목/본문 수정 시 발행 → 텍스트 벡터 재색인
public record MemoTextUpdatedEvent(
        Long memoId,
        Long userId
) {
}
