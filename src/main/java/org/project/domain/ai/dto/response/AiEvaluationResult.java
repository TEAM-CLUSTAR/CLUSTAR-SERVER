package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"relevancePass", "promptFaithfulnessScore", "groundednessScore", "taskAlignmentPass"})
public record AiEvaluationResult(
        double relevancePass,
        double promptFaithfulnessScore,
        double groundednessScore,
        boolean taskAlignmentPass
) {

    public static AiEvaluationResult of(
            double relevancePass,
            double promptFaithfulnessScore,
            double groundednessScore,
            boolean taskAlignmentPass
    ) {
        return new AiEvaluationResult(
                relevancePass,
                promptFaithfulnessScore,
                groundednessScore,
                taskAlignmentPass
        );
    }
}
