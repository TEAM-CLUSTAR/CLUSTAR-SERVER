package org.project.domain.review.ai.dto;

// AI가 돌려준 빈틈 하나(검증 전).
// 유형도 문자열로 받는다: AI가 모르는 값을 주면 파싱 전체가 실패하지 않고 그 카드만 버리기 위해서
public record FindingDraft(
        String category,
        String sectionLabel,
        String anchorText,
        String title,
        String reason,
        String evidence,
        String question
) {
}
