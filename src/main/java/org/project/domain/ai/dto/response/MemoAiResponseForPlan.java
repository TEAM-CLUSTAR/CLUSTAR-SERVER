package org.project.domain.ai.dto.response;

public record MemoAiResponseForPlan(
        MemoAiResponse aiResponse,
        AiEvaluationResult evaluation
) {

    public static MemoAiResponseForPlan of(
            MemoAiResponse aiResponse,
            AiEvaluationResult evaluation
    ) {
        return new MemoAiResponseForPlan(aiResponse, evaluation);
    }
}
