package org.project.domain.ai.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemoAiRequestForPlan(
        String userPrompt,

        @NotNull
        String systemPrompt,

        @NotEmpty
        List<@NotNull Long> memoIds
) {
    public static MemoAiRequestForPlan of(
            String userPrompt,
            String systemPrompt,
            List<Long> memoIds
    ) {
        return new MemoAiRequestForPlan(userPrompt, systemPrompt, memoIds);
    }
}

