package org.project.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReviewStartRequest(
        @Schema(description = "검토할 메모 ID", example = "3")
        @NotNull
        Long memoId
) {
}
