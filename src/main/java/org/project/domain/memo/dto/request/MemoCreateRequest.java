package org.project.domain.memo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record MemoCreateRequest(

        @Schema(description = "제목", example = "SOPT 세미나")
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Schema(description = "내용", example = "7차 세미나 내용은 ~가 중요~.")
        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        @Schema(description = "라벨 이름 목록", example = "[\"SOPT\", \"학교\", \"중요\"]")
        List<String> labelNames
) {
}
