package org.project.domain.ai.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

@Schema(requiredProperties = {"title", "content", "option", "memoIds", "usedPrompt"})
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
