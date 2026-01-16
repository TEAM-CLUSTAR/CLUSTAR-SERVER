package org.project.domain.ai.dto.response;

public record RagPromptConfigResponse(
        String systemPrompt,
        Double temperature
) {
    public static RagPromptConfigResponse of(String systemPrompt, Double temperature) {
        return new RagPromptConfigResponse(systemPrompt, temperature);
    }
}
