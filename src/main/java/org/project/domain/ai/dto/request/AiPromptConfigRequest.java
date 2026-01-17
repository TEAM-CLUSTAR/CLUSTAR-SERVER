package org.project.domain.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiPromptConfigRequest(


        @Schema(description = "시스템 프롬프트", example = "너는 나만의 똑똑한 메모 정리 AI야..~")
        String systemPrompt,

        @Schema(description = "모델의 창의성을 조절하는 파라미터", example = "0.7")
        Double temperature


) {
}
