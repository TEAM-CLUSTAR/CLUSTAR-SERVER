package org.project.domain.ai.dto.response;

import org.project.domain.ai.strategy.MemoAiOptions;

import java.util.List;

public record MemoAiResponse(
        String content,
        MemoAiOptions option,
        List<Long> memoIds,
        String usedPrompt
) {
    public static MemoAiResponse of(
            String content,
            MemoAiOptions option,
            List<Long> memoIds,
            String usedPrompt
    ) {
        return new MemoAiResponse(content, option, memoIds, usedPrompt);
    }
}

