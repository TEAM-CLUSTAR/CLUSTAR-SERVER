package org.project.domain.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomService 테스트")
class ChatRoomServiceImplTest {

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatMemory chatMemory;

    @Test
    @DisplayName("활성 채팅방이 없으면 새로 생성하고 빈 대화를 반환한다")
    void getActiveConversation_noRoom_createsRoomAndReturnsEmptyMessages() {
        // given
        when(chatRoomRepository.findTopByUserIdOrderByIdDesc(1L))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(createUser());
        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        // when
        ActiveChatRoomResponse response = chatRoomService.getActiveConversation(1L);

        // then
        assertThat(response.chatRoomId()).isEqualTo(7L);
        assertThat(response.messages()).isEmpty();
        verify(chatMessageService, never()).findByChatRoomId(anyLong());
    }

    @Test
    @DisplayName("활성 채팅방이 있으면 새로 만들지 않고 기존 대화를 반환한다")
    void getActiveConversation_roomExists_returnsExistingMessages() {
        // given
        ChatRoom existing = withId(ChatRoom.builder().user(createUser()).build(), 7L);
        when(chatRoomRepository.findTopByUserIdOrderByIdDesc(1L))
                .thenReturn(Optional.of(existing));
        when(chatMessageService.findByChatRoomId(7L))
                .thenReturn(List.of(
                        new ActiveChatRoomResponse.ChatMessageResponse(
                                1L, "USER", "SUCCESS", null, "질문", null, List.of(), null)
                ));

        // when
        ActiveChatRoomResponse response = chatRoomService.getActiveConversation(1L);

        // then
        assertThat(response.chatRoomId()).isEqualTo(7L);
        assertThat(response.messages()).hasSize(1);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("새 대화를 시작하면 기존 활성 채팅방을 삭제하고 ChatMemory를 정리한다")
    void create_existingRoom_softDeletesAndClearsChatMemory() {
        // given
        ChatRoom existing = withId(ChatRoom.builder().user(createUser()).build(), 7L);
        when(chatRoomRepository.findTopByUserIdOrderByIdDesc(1L))
                .thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(1L)).thenReturn(createUser());
        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 8L));

        // when
        ChatRoom created = chatRoomService.create(1L);

        // then
        assertThat(existing.getIsDeleted()).isTrue();
        assertThat(created.getId()).isEqualTo(8L);
        verify(chatMemory).clear("user:1:room:7");
    }

    @Test
    @DisplayName("기존 활성 채팅방이 없으면 ChatMemory 정리 없이 새 채팅방만 생성한다")
    void create_noExistingRoom_doesNotClearChatMemory() {
        // given
        when(chatRoomRepository.findTopByUserIdOrderByIdDesc(1L))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(createUser());
        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 8L));

        // when
        chatRoomService.create(1L);

        // then
        verify(chatMemory, never()).clear(anyString());
    }

    @Test
    @DisplayName("채팅방을 삭제하면 ChatMemory도 함께 정리한다")
    void delete_clearsChatMemory() {
        // given
        User user = withUserId(createUser(), 1L);
        ChatRoom chatRoom = withId(ChatRoom.builder().user(user).build(), 7L);
        when(chatRoomRepository.findById(7L)).thenReturn(Optional.of(chatRoom));

        // when
        chatRoomService.delete(1L, 7L);

        // then
        assertThat(chatRoom.getIsDeleted()).isTrue();
        verify(chatMemory).clear("user:1:room:7");
    }

    private ChatRoom withId(ChatRoom chatRoom, Long id) {
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    private User withUserId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User createUser() {
        return User.createSocialUser("test@test.com", "테스터", "profile.png", "google");
    }
}
