package org.project.domain.ai.dto;

import org.springframework.ai.chat.prompt.Prompt;

public record BuiltPrompt(
        Prompt prompt,
        String promptSnapshot
) {}
