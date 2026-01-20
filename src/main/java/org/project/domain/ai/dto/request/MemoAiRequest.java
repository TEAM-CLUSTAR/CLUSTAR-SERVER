package org.project.domain.ai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

public record MemoAiRequest(
        String userPrompt,
        MemoAiOptions option,
        @NotEmpty
        List<@NotNull Long> memoIds
) {

    public MemoAiRequest {
        if (option == null) {
            option = MemoAiOptions.DEFAULT;
        }
    }

    public static MemoAiRequest of(
            String userPrompt,
            MemoAiOptions option,
            List<Long> memoIds
    ) {
        return new MemoAiRequest(userPrompt, option, memoIds);
    }
}

