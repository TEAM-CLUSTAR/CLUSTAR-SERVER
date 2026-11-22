package org.project.domain.memo.entity;

// AI 메모를 만들 때 참고한 원본 메모의 ID와 그 시점 제목.
// 원본이 영구 삭제돼도 '사용한 메모'에 제목을 보여주기 위해 AI 메모에 함께 저장한다.
public record SourceMemoSnapshot(Long memoId, String title) {
}
