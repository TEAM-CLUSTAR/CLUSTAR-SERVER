package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MemoCreateRequest(

        @Schema(description = "제목", example = "SOPT 세미나")
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Schema(description = "내용", example = "7차 세미나 내용은 ~가 중요~.")
        @NotBlank(message = "내용을 입력해주세요.")
        String content

) {
}
