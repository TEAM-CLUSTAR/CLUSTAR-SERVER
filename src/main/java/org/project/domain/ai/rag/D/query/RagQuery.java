package org.project.domain.ai.rag.D.query;


import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

public record RagQuery(
        Long userId,
        MemoAiOptions option,
        String userPrompt,
        List<Long> memoIds
) {
    public static RagQuery of(
            Long userId,
            MemoAiOptions option,
            String userPrompt,
            List<Long> memoIds
    ) {
        return new RagQuery(userId, option, userPrompt, memoIds);
    }
}
