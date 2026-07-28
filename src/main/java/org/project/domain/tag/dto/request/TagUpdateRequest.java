package org.project.domain.tag.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TagUpdateRequest(
        @Schema(description = "태그 이름", example = "SOPT")
        @NotBlank(message = "태그 이름은 필수입니다.")
        String name
) {
}
