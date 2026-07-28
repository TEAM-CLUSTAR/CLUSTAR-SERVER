package org.project.domain.ai.dto.response;

import org.project.domain.ai.entity.EmbeddingFailure;

import java.time.LocalDateTime;

public record EmbeddingFailureResponse(
        Long id,
        Long memoId,
        String embeddingType,
        LocalDateTime failedAt,
        String errorMessage
) {
    public static EmbeddingFailureResponse from(EmbeddingFailure failure) {
        return new EmbeddingFailureResponse(
                failure.getId(),
                failure.getMemoId(),
                failure.getEmbeddingType(),
                failure.getFailedAt(),
                failure.getErrorMessage()
        );
    }
}
