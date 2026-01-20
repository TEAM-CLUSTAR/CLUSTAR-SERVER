package org.project.domain.ai.service;

import org.project.domain.ai.entity.ChatRoom;

public interface ChatRoomService {

    ChatRoom create(Long userId);

    void delete(Long userId, Long chatRoomId);
}
