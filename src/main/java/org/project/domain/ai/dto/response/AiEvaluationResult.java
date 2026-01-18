package org.project.domain.ai.dto.response;

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
