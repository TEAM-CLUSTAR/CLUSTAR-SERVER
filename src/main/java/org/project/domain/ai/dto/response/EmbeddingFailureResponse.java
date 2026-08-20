package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.ai.entity.EmbeddingFailure;

import java.time.LocalDateTime;

@Schema(requiredProperties = {"id", "memoId", "embeddingType", "failedAt", "errorMessage"})
public record EmbeddingFailureResponse(
        Long id,
        Long memoId,
        String embeddingType,
        LocalDateTime failedAt,
        @Schema(nullable = true)
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
