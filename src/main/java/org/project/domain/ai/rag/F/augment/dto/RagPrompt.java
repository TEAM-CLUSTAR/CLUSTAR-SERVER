package org.project.domain.ai.rag.F.augment.dto;

import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;

public record RagPrompt(
        String systemPrompt,
        String userPrompt,
        String context,
        Long userId,
        Long chatRoomId
) {

    public static RagPrompt of(
            String systemPrompt,
            String userPrompt,
            String context
    ) {
        return new RagPrompt(
                systemPrompt,
                userPrompt,
                context,
                null,
                null
        );
    }

    // Conversation Context 주입
    public RagPrompt withConversationContext(Long userId, Long chatRoomId) {
        return new RagPrompt(
                this.systemPrompt,
                this.userPrompt,
                this.context,
                userId,
                chatRoomId
        );
    }

    // ChatMemory에서 사용할 conversationId
    public String conversationId() {
        if (userId == null || chatRoomId == null) {
            throw new AiException(AiErrorCode.CONVERSATION_CONTEXT_NOT_SET);
        }

        return "user:%d:room:%d".formatted(userId, chatRoomId);
    }

    // systemPrompt만 교체 (Plan API 용)
    public RagPrompt withSystemPrompt(String newSystemPrompt) {
        return new RagPrompt(
                newSystemPrompt,
                this.userPrompt,
                this.context,
                this.userId,
                this.chatRoomId
        );
    }

    // API 응답 / 로그 / 디버깅용
    public String toDebugString() {
        return """
                [SYSTEM PROMPT]
                %s

                [USER PROMPT]
                %s

                [CONTEXT]
                %s

                [CONVERSATION]
                userId=%s, chatRoomId=%s
                """.formatted(
                systemPrompt,
                userPrompt,
                context,
                userId,
                chatRoomId
        );
    }
}
