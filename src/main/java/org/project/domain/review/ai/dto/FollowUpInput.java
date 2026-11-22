package org.project.domain.review.ai.dto;

// 후속 질문을 만들 때 AI에게 넘기는 재료(본문, 카드, 첫 질문, 사용자의 첫 답)
public record FollowUpInput(
        Long memoId,
        String memoTitle,
        String memoContent,
        String sectionLabel,
        String anchorText,
        String reason,
        String evidence,
        String question,
        String answer
) {
}
