package org.project.domain.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.project.domain.ai.strategy.MemoAiOptions;

import java.util.List;

public record AiMemoCreateRequest(

        @NotBlank
        @Schema(description = "유저 프롬프트", example = "메모를 감성적으로 정리해줘.")
        String userPrompt,

        @Schema(description = "유사도 검색 시 반환할 청크 개수 (많을수록 더 많은 컨텍스트 제공)", example = "5")
        Integer topK,

        @NotNull
        @Schema(description = "AI의 성격을 정하는 옵션", example = "MERGE")
        MemoAiOptions option,


        @NotEmpty
        @Schema(description = "참고할 메모들의 ID", example = "[1,2,3]")
        List<Long> memoIds,

        @Schema(description = "프롬프트 커스터마이징 설정")
        AiPromptConfigRequest promptConfig
) {
}
