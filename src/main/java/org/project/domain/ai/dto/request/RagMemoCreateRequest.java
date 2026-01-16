package org.project.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.project.domain.ai.strategy.MemoAiOptions;

import java.util.List;

public record RagMemoCreateRequest(
        @NotBlank
        String userPrompt,
        Integer topK,
        @NotNull
        MemoAiOptions option,
        @NotEmpty
        List<Long> memoIds,
        RagPromptConfigRequest promptConfig
) {
}
