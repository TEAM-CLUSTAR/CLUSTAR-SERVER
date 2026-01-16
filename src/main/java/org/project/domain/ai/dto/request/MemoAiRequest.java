package org.project.domain.ai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

public record MemoAiRequest(
        String userPrompt,

        @NotNull
        MemoAiOptions option,

        @NotEmpty
        List<@NotNull Long> memoIds
) {
    public static MemoAiRequest of(
            String userPrompt,
            MemoAiOptions option,
            List<Long> memoIds
    ) {
        return new MemoAiRequest(userPrompt, option, memoIds);
    }
}

