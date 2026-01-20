package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.ChatRoomErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoom create(Long userId) {
        User user = userRepository.getReferenceById(userId);

        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void delete(Long userId, Long chatRoomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new AiException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        if (!chatRoom.getUser().getId().equals(userId)) {
            throw new AiException(ChatRoomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(chatRoom.getIsDeleted())) {
            throw new AiException(ChatRoomErrorCode.CHAT_ROOM_ALREADY_DELETED);
        }

        chatRoom.markDeleted(); // isDeleted = true
    }
}
