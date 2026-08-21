package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = "message")
public record ReEmbeddingStartedResponse(
        String message
) {
}
