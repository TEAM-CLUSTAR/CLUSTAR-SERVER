package org.project.domain.ai.util;

/**
 * Spring AI ChatMemory의 conversationId 규칙을 한곳에서 관리한다.
 */
public final class ConversationIds {

    private static final String FORMAT = "user:%d:room:%d";

    private ConversationIds() {
    }

    public static String of(Long userId, Long chatRoomId) {
        return FORMAT.formatted(userId, chatRoomId);
    }
}
