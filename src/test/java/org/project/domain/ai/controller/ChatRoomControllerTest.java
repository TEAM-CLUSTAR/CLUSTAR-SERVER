package org.project.domain.ai.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.dto.response.ChatRoomListResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.service.ChatRoomService;
import org.project.global.exception.domainException.ChatRoomException;
import org.project.global.exception.errorcode.ChatRoomErrorCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.entity.User;
import org.project.global.security.filter.JWTFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChatRoomController 테스트")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private JWTFilter jwtFilter;

    @Test
    @DisplayName("대화가 없는 채팅방은 빈 messages 배열을 반환한다")
    @WithMockCustomUser
    void getActiveConversation_emptyRoom() throws Exception {
        // given
        when(chatRoomService.getActiveConversation(1L))
                .thenReturn(ActiveChatRoomResponse.of(7L, List.of()));

        // when & then
        mockMvc.perform(get("/api/v1/chat-rooms/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value(7L))
                .andExpect(jsonPath("$.data.messages").isEmpty());
    }

    @Test
    @DisplayName("대화가 있는 채팅방은 사용자 프롬프트와 AI 답변을 순서대로 반환한다")
    @WithMockCustomUser
    void getActiveConversation_withMessages() throws Exception {
        // given
        when(chatRoomService.getActiveConversation(1L)).thenReturn(
                ActiveChatRoomResponse.of(7L, List.of(
                        new ActiveChatRoomResponse.ChatMessageResponse(
                                1L, "USER", "SUCCESS", null, "이 메모들 정리해줘",
                                MemoAiOptions.MERGE, List.of(11L, 12L), LocalDateTime.now()),
                        new ActiveChatRoomResponse.ChatMessageResponse(
                                2L, "ASSISTANT", "SUCCESS", "주간 회의 정리", "본문",
                                MemoAiOptions.MERGE, List.of(11L, 12L), LocalDateTime.now())
                ))
        );

        // when & then
        mockMvc.perform(get("/api/v1/chat-rooms/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.messages[1].title").value("주간 회의 정리"))
                .andExpect(jsonPath("$.data.messages[1].option").value("MERGE"))
                .andExpect(jsonPath("$.data.messages[1].memoIds[0]").value(11L));
    }

    @Test
    @DisplayName("AI 호출이 실패한 턴은 FAILED 상태로, 재시도에 필요한 요청 정보와 함께 반환된다")
    @WithMockCustomUser
    void getActiveConversation_failedTurn() throws Exception {
        // given
        when(chatRoomService.getActiveConversation(1L)).thenReturn(
                ActiveChatRoomResponse.of(7L, List.of(
                        new ActiveChatRoomResponse.ChatMessageResponse(
                                3L, "USER", "SUCCESS", null, "정리해줘",
                                MemoAiOptions.STRUCTURE, List.of(11L, 12L), LocalDateTime.now()),
                        new ActiveChatRoomResponse.ChatMessageResponse(
                                4L, "ASSISTANT", "FAILED", null, null,
                                null, List.of(), LocalDateTime.now())
                ))
        );

        // when & then
        mockMvc.perform(get("/api/v1/chat-rooms/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].option").value("STRUCTURE"))
                .andExpect(jsonPath("$.data.messages[0].memoIds[0]").value(11L))
                .andExpect(jsonPath("$.data.messages[1].status").value("FAILED"))
                .andExpect(jsonPath("$.data.messages[1].content").doesNotExist());
    }

    @Test
    @DisplayName("새 대화를 시작하면 생성된 채팅방 ID를 반환한다")
    @WithMockCustomUser
    void createChatRoom_returnsNewChatRoomId() throws Exception {
        // given
        ChatRoom created = ChatRoom.builder().build();
        ReflectionTestUtils.setField(created, "id", 8L);
        when(chatRoomService.create(1L)).thenReturn(created);

        // when & then
        mockMvc.perform(post("/api/v1/chat-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value(8L));
    }

    @Test
    @DisplayName("채팅방 목록을 반환한다")
    @WithMockCustomUser
    void getChatRooms_returnsList() throws Exception {
        // given
        when(chatRoomService.findAllByUser(1L)).thenReturn(
                ChatRoomListResponse.of(List.of(
                        ChatRoomListResponse.ChatRoomResponse.of(7L, LocalDateTime.now()),
                        ChatRoomListResponse.ChatRoomResponse.of(8L, LocalDateTime.now())
                ))
        );

        // when & then
        mockMvc.perform(get("/api/v1/chat-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRooms.length()").value(2))
                .andExpect(jsonPath("$.data.chatRooms[0].chatRoomId").value(7L));
    }

    @Test
    @DisplayName("최근 채팅방이 없으면 404를 반환한다")
    @WithMockCustomUser
    void findLatestChatRoom_notFound() throws Exception {
        // given
        when(chatRoomService.findLatestChatRoomByUser(1L))
                .thenThrow(new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/chat-rooms/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("채팅방을 삭제한다")
    @WithMockCustomUser
    void deleteChatRoom_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/chat-rooms/7"))
                .andExpect(status().isOk());

        verify(chatRoomService).delete(1L, 7L);
    }

    @Test
    @DisplayName("남의 채팅방을 삭제하면 403을 반환한다")
    @WithMockCustomUser
    void deleteChatRoom_accessDenied() throws Exception {
        // given
        doThrow(new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_ACCESS_DENIED))
                .when(chatRoomService).delete(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/chat-rooms/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
    @interface WithMockCustomUser {
        long userId() default 1L;
    }

    static class WithMockCustomUserSecurityContextFactory
            implements WithSecurityContextFactory<WithMockCustomUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            User user = User.builder()
                    .id(annotation.userId())
                    .email("test@example.com")
                    .name("테스트유저")
                    .providerName("google")
                    .profileImageUrl(null)
                    .build();

            CustomUserDetails userDetails = new CustomUserDetails(user);
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            ));
            return context;
        }
    }
}
