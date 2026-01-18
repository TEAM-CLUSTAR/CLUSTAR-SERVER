package org.project.domain.ai.dto.response;


import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

public record MemoAiResponse(
        String title,
        String content,
        MemoAiOptions option,
        List<Long> memoIds,
        String usedPrompt
) {
    public static MemoAiResponse of(
            String title,
            String content,
            MemoAiOptions option,
            List<Long> memoIds,
            String usedPrompt
    ) {
        return new MemoAiResponse(title, content, option, memoIds, usedPrompt);
    }
}
