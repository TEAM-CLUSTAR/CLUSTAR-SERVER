package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.dto.response.ChatRoomListResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.project.domain.ai.util.ConversationIds;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.ChatRoomException;
import org.project.global.exception.errorcode.ChatRoomErrorCode;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageService chatMessageService;
    private final ChatMemory chatMemory;

    /**
     * AI 패널 진입용. 활성 채팅방과 대화 목록을 함께 반환한다.
     */
    @Override
    @Transactional
    public ActiveChatRoomResponse getActiveConversation(Long userId) {

        Optional<ChatRoom> activeChatRoom = findActiveChatRoom(userId);

        if (activeChatRoom.isEmpty()) {
            ChatRoom created = save(userId);
            return ActiveChatRoomResponse.of(created.getId(), List.of());
        }

        Long chatRoomId = activeChatRoom.get().getId();

        return ActiveChatRoomResponse.of(
                chatRoomId,
                chatMessageService.findByChatRoomId(chatRoomId)
        );
    }

    /**
     * 새 대화 시작. 기존 활성 채팅방과 대화 컨텍스트를 정리한 뒤 새 채팅방을 만든다.
     */
    @Override
    @Transactional
    public ChatRoom create(Long userId) {

        findActiveChatRoom(userId)
                .ifPresent(previous -> clearConversation(userId, previous));

        return save(userId);
    }


    @Override
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

        ChatRoom chatRoom = findActiveChatRoom(userId)
                .orElseThrow(() -> new ChatRoomException(
                        ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND
                ));

        return ChatRoomListResponse.ChatRoomResponse.from(chatRoom);
    }


    @Override
    @Transactional
    public void delete(Long userId, Long chatRoomId) {

        ChatRoom chatRoom = validateAccess(userId, chatRoomId);

        clearConversation(userId, chatRoom);
    }


    /**
     * 공통 검증 메서드
     * <p>
     * ChatRoom의 @Where(is_deleted = false)로 삭제된 채팅방은 조회되지 않으므로,
     * 삭제된 채팅방에 접근하면 CHAT_ROOM_NOT_FOUND로 응답한다.
     */
    @Override
    public ChatRoom validateAccess(Long userId, Long chatRoomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() ->
                        new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        if (!chatRoom.getUser().getId().equals(userId)) {
            throw new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        return chatRoom;
    }


    /**
     * 채팅방은 soft delete하고, Spring AI가 관리하는 대화 컨텍스트는 물리 삭제한다.
     * (ChatMemory는 clear() 외에 삭제 수단을 제공하지 않는다)
     */
    private void clearConversation(Long userId, ChatRoom chatRoom) {
        chatRoom.markDeleted();
        chatMemory.clear(ConversationIds.of(userId, chatRoom.getId()));
    }


    /**
     * 활성 채팅방 조회. ChatRoom의 @Where(is_deleted = false)로 삭제된 방은 제외된다.
     * 삭제와 생성이 한 트랜잭션에서 일어나면 createdAt이 동률로 기록될 수 있어 id를 기준으로 정렬한다.
     */
    private Optional<ChatRoom> findActiveChatRoom(Long userId) {
        return chatRoomRepository.findTopByUserIdOrderByIdDesc(userId);
    }


    private ChatRoom save(Long userId) {
        User user = userRepository.getReferenceById(userId);

        return chatRoomRepository.save(
                ChatRoom.builder()
                        .user(user)
                        .build()
        );
    }
}
