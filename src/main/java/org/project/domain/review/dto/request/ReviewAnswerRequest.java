package org.project.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ReviewAnswerRequest(
        @Schema(description = "사용자의 답(1000자 이하)", example = "음 1~2학년이 기초 전공책을 제일 많이 사서")
        @NotBlank
        String content
) {
}
