package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"aiResponse", "evaluation"})
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
