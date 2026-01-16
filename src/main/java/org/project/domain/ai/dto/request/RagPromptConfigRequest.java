package org.project.domain.ai.dto.request;

public record RagPromptConfigRequest(
        String systemPrompt,
        Double temperature
) {
}
