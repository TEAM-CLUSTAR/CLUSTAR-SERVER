package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.ai.entity.ChatRoom;

import java.time.LocalDateTime;
import java.util.List;

@Schema(requiredProperties = "chatRooms")
public record ChatRoomListResponse(
        List<ChatRoomResponse> chatRooms
) {

    public static ChatRoomListResponse of(List<ChatRoomResponse> chatRooms) {
        return new ChatRoomListResponse(chatRooms);
    }

    @Schema(requiredProperties = {"chatRoomId", "createdAt"})
    public record ChatRoomResponse(
            Long chatRoomId,
            LocalDateTime createdAt
    ) {
        public static ChatRoomResponse of(Long chatRoomId, LocalDateTime createdAt) {
            return new ChatRoomResponse(chatRoomId, createdAt);
        }

        public static ChatRoomResponse from(ChatRoom chatRoom) {
            return new ChatRoomResponse(chatRoom.getId(), chatRoom.getCreatedAt());
        }
    }
}
