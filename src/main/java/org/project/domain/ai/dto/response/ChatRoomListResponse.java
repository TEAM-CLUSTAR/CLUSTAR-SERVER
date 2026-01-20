package org.project.domain.ai.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomListResponse(
        List<ChatRoomResponse> chatRooms
) {

    public static ChatRoomListResponse of(List<ChatRoomResponse> chatRooms) {
        return new ChatRoomListResponse(chatRooms);
    }

    public record ChatRoomResponse(
            Long chatRoomId,
            LocalDateTime createdAt
    ) {
        public static ChatRoomResponse of(Long chatRoomId, LocalDateTime createdAt) {
            return new ChatRoomResponse(chatRoomId, createdAt);
        }
    }
}

