package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = "chatRoomId")
public record CreateChatRoomResponse(
        Long chatRoomId
) {
    public static CreateChatRoomResponse of(Long chatRoomId) {
        return new CreateChatRoomResponse(chatRoomId);
    }
}
