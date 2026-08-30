package org.project.domain.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.ActiveChatRoomResponse;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.ai.repository.ChatMessageRepository;
import org.project.domain.ai.repository.ChatRoomRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저장이 본업인 서비스라 목이 아닌 실제 영속화 결과로 검증한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslTestConfig.class, ChatMessageServiceImpl.class})
@DisplayName("ChatMessageService 테스트")
class ChatMessageServiceImplTest {

    @Autowired
    private ChatMessageServiceImpl chatMessageService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Nested
    @DisplayName("saveTurn")
    class SaveTurn {

        @Test
        @DisplayName("사용자 질문과 AI 응답을 순서대로 두 건 저장한다")
        void savesUserAndAssistantInOrder() {
            // given
            Long chatRoomId = createChatRoom().getId();
            MemoAiRequest request = MemoAiRequest.of(
                    "이 메모들 정리해줘", MemoAiOptions.MERGE, List.of(11L, 12L));
            MemoAiResponse response = MemoAiResponse.of(
                    "주간 회의 정리", "정리된 본문", MemoAiOptions.MERGE, List.of(11L, 12L), null);

            // when
            chatMessageService.saveTurn(chatRoomId, request, response);
            flushAndClear();

            // then
            List<ActiveChatRoomResponse.ChatMessageResponse> messages =
                    chatMessageService.findByChatRoomId(chatRoomId);

            assertThat(messages).hasSize(2);
            assertThat(messages.get(0).role()).isEqualTo("USER");
            assertThat(messages.get(0).content()).isEqualTo("이 메모들 정리해줘");
            assertThat(messages.get(1).role()).isEqualTo("ASSISTANT");
            assertThat(messages.get(1).title()).isEqualTo("주간 회의 정리");
            assertThat(messages.get(1).content()).isEqualTo("정리된 본문");
        }

        @Test
        @DisplayName("사용자 메시지에도 재시도에 필요한 option과 memoIds를 보관한다")
        void userMessageKeepsRequestContext() {
            // given
            Long chatRoomId = createChatRoom().getId();

            // when
            chatMessageService.saveTurn(
                    chatRoomId,
                    MemoAiRequest.of("구조화해줘", MemoAiOptions.STRUCTURE, List.of(21L, 22L)),
                    MemoAiResponse.of("제목", "본문", MemoAiOptions.STRUCTURE, List.of(21L, 22L), null)
            );
            flushAndClear();

            // then
            var userMessage = chatMessageService.findByChatRoomId(chatRoomId).get(0);
            assertThat(userMessage.option()).isEqualTo(MemoAiOptions.STRUCTURE);
            assertThat(userMessage.memoIds()).containsExactly(21L, 22L);
        }

        @Test
        @DisplayName("두 건 모두 SUCCESS 상태로 저장된다")
        void bothMessagesAreSuccess() {
            // given
            Long chatRoomId = createChatRoom().getId();

            // when
            chatMessageService.saveTurn(
                    chatRoomId,
                    MemoAiRequest.of("정리해줘", MemoAiOptions.MERGE, List.of(11L)),
                    MemoAiResponse.of("제목", "본문", MemoAiOptions.MERGE, List.of(11L), null)
            );
            flushAndClear();

            // then
            assertThat(chatMessageService.findByChatRoomId(chatRoomId))
                    .extracting(ActiveChatRoomResponse.ChatMessageResponse::status)
                    .containsExactly("SUCCESS", "SUCCESS");
        }
    }

    @Nested
    @DisplayName("saveFailedTurn")
    class SaveFailedTurn {

        @Test
        @DisplayName("사용자 질문은 남기고 AI 응답 자리는 FAILED로 비워 둔다")
        void keepsUserPromptAndMarksAssistantFailed() {
            // given
            Long chatRoomId = createChatRoom().getId();

            // when
            chatMessageService.saveFailedTurn(
                    chatRoomId,
                    MemoAiRequest.of("정리해줘", MemoAiOptions.SUMMARY, List.of(31L, 32L))
            );
            flushAndClear();

            // then
            List<ActiveChatRoomResponse.ChatMessageResponse> messages =
                    chatMessageService.findByChatRoomId(chatRoomId);

            assertThat(messages).hasSize(2);

            var userMessage = messages.get(0);
            assertThat(userMessage.status()).isEqualTo("SUCCESS");
            assertThat(userMessage.content()).isEqualTo("정리해줘");

            var failedMessage = messages.get(1);
            assertThat(failedMessage.role()).isEqualTo("ASSISTANT");
            assertThat(failedMessage.status()).isEqualTo("FAILED");
            assertThat(failedMessage.content()).isNull();
            assertThat(failedMessage.title()).isNull();
        }

        @Test
        @DisplayName("실패해도 재시도할 수 있도록 사용자 메시지에 요청 정보를 남긴다")
        void failedTurnKeepsRetryContext() {
            // given
            Long chatRoomId = createChatRoom().getId();

            // when
            chatMessageService.saveFailedTurn(
                    chatRoomId,
                    MemoAiRequest.of("정리해줘", MemoAiOptions.SUMMARY, List.of(31L, 32L))
            );
            flushAndClear();

            // then
            var userMessage = chatMessageService.findByChatRoomId(chatRoomId).get(0);
            assertThat(userMessage.option()).isEqualTo(MemoAiOptions.SUMMARY);
            assertThat(userMessage.memoIds()).containsExactly(31L, 32L);
        }

        @Test
        @DisplayName("실패한 턴이 반복되면 그대로 누적된다")
        void repeatedFailuresAccumulate() {
            // given
            Long chatRoomId = createChatRoom().getId();
            MemoAiRequest request = MemoAiRequest.of("정리해줘", MemoAiOptions.MERGE, List.of(41L));

            // when
            chatMessageService.saveFailedTurn(chatRoomId, request);
            chatMessageService.saveFailedTurn(chatRoomId, request);
            flushAndClear();

            // then
            assertThat(chatMessageService.findByChatRoomId(chatRoomId))
                    .extracting(ActiveChatRoomResponse.ChatMessageResponse::status)
                    .containsExactly("SUCCESS", "FAILED", "SUCCESS", "FAILED");
        }
    }

    @Nested
    @DisplayName("findByChatRoomId")
    class FindByChatRoomId {

        @Test
        @DisplayName("대화가 없으면 빈 목록을 반환한다")
        void emptyRoomReturnsEmptyList() {
            assertThat(chatMessageService.findByChatRoomId(createChatRoom().getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("다른 채팅방의 메시지는 섞이지 않는다")
        void doesNotLeakOtherRoomMessages() {
            // given
            User user = userRepository.save(createUser());
            Long roomA = chatRoomRepository.save(ChatRoom.builder().user(user).build()).getId();
            Long roomB = chatRoomRepository.save(ChatRoom.builder().user(user).build()).getId();

            chatMessageService.saveTurn(
                    roomA,
                    MemoAiRequest.of("A방 질문", MemoAiOptions.MERGE, List.of(11L)),
                    MemoAiResponse.of("A제목", "A본문", MemoAiOptions.MERGE, List.of(11L), null));
            chatMessageService.saveFailedTurn(
                    roomB, MemoAiRequest.of("B방 질문", MemoAiOptions.MERGE, List.of(12L)));
            flushAndClear();

            // then
            assertThat(chatMessageService.findByChatRoomId(roomA))
                    .extracting(ActiveChatRoomResponse.ChatMessageResponse::content)
                    .containsExactly("A방 질문", "A본문");
        }

        @Test
        @DisplayName("여러 턴은 주고받은 순서대로 반환된다")
        void returnsMessagesInConversationOrder() {
            // given
            Long chatRoomId = createChatRoom().getId();

            chatMessageService.saveTurn(
                    chatRoomId,
                    MemoAiRequest.of("첫 질문", MemoAiOptions.MERGE, List.of(11L)),
                    MemoAiResponse.of("첫 제목", "첫 답변", MemoAiOptions.MERGE, List.of(11L), null));
            chatMessageService.saveTurn(
                    chatRoomId,
                    MemoAiRequest.of("둘째 질문", MemoAiOptions.SUMMARY, List.of(11L)),
                    MemoAiResponse.of("둘째 제목", "둘째 답변", MemoAiOptions.SUMMARY, List.of(11L), null));
            flushAndClear();

            // then
            assertThat(chatMessageService.findByChatRoomId(chatRoomId))
                    .extracting(ActiveChatRoomResponse.ChatMessageResponse::content)
                    .containsExactly("첫 질문", "첫 답변", "둘째 질문", "둘째 답변");
        }
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private ChatRoom createChatRoom() {
        return chatRoomRepository.save(
                ChatRoom.builder().user(userRepository.save(createUser())).build()
        );
    }

    private User createUser() {
        return User.createSocialUser(
                "test" + UUID.randomUUID() + "@test.com", "테스트 유저", "profile.png", "google");
    }
}
