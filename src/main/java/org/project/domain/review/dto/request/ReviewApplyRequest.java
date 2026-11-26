package org.project.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReviewApplyRequest(
        @Schema(description = "REFINED: 다듬은 문장 반영 / RAW: 내 답 원문 그대로 반영", example = "REFINED")
        @NotNull
        ApplyType type,

        @Schema(description = "REFINED일 때 넣을 최종 문장(미리보기에서 고친 그대로). RAW면 보내지 않아도 된다",
                example = "선정 기준: 1~2학년이 기초 전공책을 제일 많이 사기 때문이다.")
        String text
) {
}
