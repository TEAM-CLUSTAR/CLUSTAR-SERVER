package org.project.domain.label.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LabelCreateRequest(
        @Schema(description = "태그 이름", example = "SOPT")
        @NotBlank(message = "태그 이름은 필수입니다.")
        String name,

        @Schema(description = "부모 태그 ID", example = "1")
        Long parentLabelId
) {
}
