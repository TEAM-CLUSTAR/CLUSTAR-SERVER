package org.project.domain.memo.event;

/**
 * 메모 제목/본문이 수정되었을 때 발행. 텍스트 벡터를 재색인한다.
 * (기존 텍스트 벡터 삭제 후 새 벡터 적재 — 순서/원자성은 리스너가 보장)
 */
public record MemoTextUpdatedEvent(
        Long memoId,
        Long userId
) {
}
