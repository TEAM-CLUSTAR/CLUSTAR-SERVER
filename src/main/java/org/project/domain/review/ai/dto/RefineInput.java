package org.project.domain.review.ai.dto;

import java.util.List;

// 다듬기 재료: 짚은 문장과 질문별 답(답한 질문만)
public record RefineInput(
        Long memoId,
        String sectionLabel,
        String anchorText,
        List<QuestionAnswers> questionAnswers
) {

    // 내 답 원문: 보낸 순서대로 줄바꿈으로 잇는다([원문 그대로 넣기]도 이 문장을 쓴다)
    public String rawText() {
        return String.join("\n", questionAnswers.stream()
                .flatMap(qa -> qa.answers().stream())
                .toList());
    }
}
