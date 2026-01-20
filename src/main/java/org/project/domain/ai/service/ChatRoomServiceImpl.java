package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.response.ChatRoomListResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.domainException.ChatRoomException;
import org.project.global.exception.errorcode.ChatRoomErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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


    public ChatRoomListResponse findAllByUser(Long userId) {
        return ChatRoomListResponse.of(
                chatRoomRepository.findAllByUserIdAndIsDeletedFalse(userId)
                        .stream()
                        .map(chatRoom ->
                                ChatRoomListResponse.ChatRoomResponse.of(
                                        chatRoom.getId(),
                                        chatRoom.getCreatedAt()
                                )
                        )
                        .toList()
        );
    }


    @Override
    public ChatRoomListResponse.ChatRoomResponse findLatestChatRoomByUser(Long userId) {

        ChatRoom chatRoom = chatRoomRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new ChatRoomException(
                        ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND
                ));

        return ChatRoomListResponse.ChatRoomResponse.from(chatRoom);
    }


    @Transactional
    public void delete(Long userId, Long chatRoomId) {

        ChatRoom chatRoom = validateAccess(userId, chatRoomId);

        chatRoom.markDeleted(); // isDeleted = true
    }


    /**
     * 공통 검증 메서드
     * @param userId
     * @param chatRoomId
     * @return
     */
    public ChatRoom validateAccess(Long userId, Long chatRoomId) {

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

        return chatRoom;
    }
}
