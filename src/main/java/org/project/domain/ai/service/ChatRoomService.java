package org.project.domain.ai.service;

import org.project.domain.ai.dto.response.ChatRoomListResponse;
import org.project.domain.ai.entity.ChatRoom;

public interface ChatRoomService {

    ChatRoom create(Long userId);

    ChatRoomListResponse findAllByUser(Long userId);

    ChatRoomListResponse.ChatRoomResponse findLatestChatRoomByUser(Long userId);

    void delete(Long userId, Long chatRoomId);

    ChatRoom validateAccess(Long userId, Long chatRoomId);
}
