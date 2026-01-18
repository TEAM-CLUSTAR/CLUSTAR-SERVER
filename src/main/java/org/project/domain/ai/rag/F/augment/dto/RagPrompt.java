package org.project.domain.ai.rag.F.augment.dto;

public record RagPrompt(
        String systemPrompt,
        String userPrompt,
        String context
) {

    public static RagPrompt of(
            String systemPrompt,
            String userPrompt,
            String context
    ) {
        return new RagPrompt(systemPrompt, userPrompt, context);
    }

    /**
     * 🔍 API 응답 / 로그 / 디버깅용
     */
    public String toDebugString() {
        return """
                [SYSTEM PROMPT]
                %s

                [USER PROMPT]
                %s

                [CONTEXT]
                %s
                """.formatted(systemPrompt, userPrompt, context);
    }

    /**
     * systemPrompt만 교체 (Plan API 용 메서드)
     */
    public RagPrompt withSystemPrompt(String newSystemPrompt) {
        return new RagPrompt(
                newSystemPrompt,
                this.userPrompt,
                this.context
        );
    }
}
