package org.project.domain.ai.strategy;

import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

public interface MemoAiStrategy {

    MemoAiOptions supports();

    Prompt buildPrompt(
            List<String> memos,
            MemoAiOptions option,
            String userPrompt
    );
}

