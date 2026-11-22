package org.project.domain.review.support;

import org.project.domain.review.entity.FindingCategory;

// 검증을 통과한 빈틈. anchorText는 본문에 실제로 적힌 문장이다
public record ValidFindingDraft(
        FindingCategory category,
        String sectionLabel,
        String anchorText,
        String title,
        String reason,
        String evidence,
        String question
) {
}
