package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        if (!chatRoom.getUser().getId().equals(userId)) {
            throw new IllegalStateException("채팅방 삭제 권한이 없습니다.");
        }

        if (Boolean.TRUE.equals(chatRoom.getIsDeleted())) {
            return; // 또는 예외
        }

        chatRoom.markDeleted(); // isDeleted = true
    }
}
