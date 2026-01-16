package org.project.domain.ai.strategy;

import org.project.domain.ai.dto.BuiltPrompt;

public interface MemoAiStrategy {

    MemoAiOptions supports();

    BuiltPrompt buildPrompt(
            String context,
            MemoAiOptions option,
            String userPrompt
    );
}

