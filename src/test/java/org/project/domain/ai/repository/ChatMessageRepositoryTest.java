package org.project.domain.ai.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.ai.dto.MemoAiOptions;
import org.project.domain.ai.entity.ChatMessage;
import org.project.domain.ai.entity.ChatMessageRole;
import org.project.domain.ai.entity.ChatMessageStatus;
import org.project.domain.ai.entity.ChatRoom;
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

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("ChatMessageRepository 테스트")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("같은 트랜잭션에서 저장한 메시지도 id 오름차순으로 조회된다")
    void findByChatRoomIdOrderByIdAsc_sameTransaction_keepsInsertOrder() {
        // given
        ChatRoom chatRoom = createChatRoom();

        chatMessageRepository.save(ChatMessage.user(
                chatRoom, "이 메모들 정리해줘", MemoAiOptions.MERGE, List.of(11L, 12L)));
        chatMessageRepository.save(ChatMessage.assistant(
                chatRoom, "주간 회의 정리", "정리된 본문",
                MemoAiOptions.MERGE, List.of(11L, 12L, 13L)
        ));
        em.flush();
        em.clear();

        // when
        List<ChatMessage> messages =
                chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoom.getId());

        // then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(ChatMessageRole.USER);
        assertThat(messages.get(1).getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
    }

    @Test
    @DisplayName("ASSISTANT 메시지의 memoIds는 저장 후 그대로 복원된다")
    void memoIds_roundTrip() {
        // given
        ChatRoom chatRoom = createChatRoom();
        chatMessageRepository.save(ChatMessage.assistant(
                chatRoom, "제목", "본문", MemoAiOptions.SUMMARY, List.of(11L, 12L, 13L)
        ));
        em.flush();
        em.clear();

        // when
        ChatMessage found =
                chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoom.getId()).get(0);

        // then
        assertThat(found.getMemoIds()).containsExactly(11L, 12L, 13L);
        assertThat(found.getOption()).isEqualTo(MemoAiOptions.SUMMARY);
        assertThat(found.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("USER 메시지는 재시도에 필요한 option과 memoIds를 함께 보관한다")
    void userMessage_keepsRequestContext() {
        // given
        ChatRoom chatRoom = createChatRoom();
        chatMessageRepository.save(ChatMessage.user(
                chatRoom, "정리해줘", MemoAiOptions.STRUCTURE, List.of(11L, 12L)));
        em.flush();
        em.clear();

        // when
        ChatMessage found =
                chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoom.getId()).get(0);

        // then
        assertThat(found.getOption()).isEqualTo(MemoAiOptions.STRUCTURE);
        assertThat(found.getMemoIds()).containsExactly(11L, 12L);
        assertThat(found.getStatus()).isEqualTo(ChatMessageStatus.SUCCESS);
    }

    @Test
    @DisplayName("실패한 AI 응답은 content 없이 FAILED 상태로 저장된다")
    void failedAssistantMessage_hasNoContent() {
        // given
        ChatRoom chatRoom = createChatRoom();
        chatMessageRepository.save(ChatMessage.failed(chatRoom));
        em.flush();
        em.clear();

        // when
        ChatMessage found =
                chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoom.getId()).get(0);

        // then
        assertThat(found.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(found.getStatus()).isEqualTo(ChatMessageStatus.FAILED);
        assertThat(found.getContent()).isNull();
        assertThat(found.getTitle()).isNull();
    }

    @Test
    @DisplayName("USER 메시지는 title 없이 저장된다")
    void userMessage_hasNoAssistantFields() {
        // given
        ChatRoom chatRoom = createChatRoom();
        chatMessageRepository.save(ChatMessage.user(
                chatRoom, "질문", MemoAiOptions.MERGE, List.of(11L)));
        em.flush();
        em.clear();

        // when
        ChatMessage found =
                chatMessageRepository.findByChatRoomIdOrderByIdAsc(chatRoom.getId()).get(0);

        // then
        assertThat(found.getContent()).isEqualTo("질문");
        assertThat(found.getTitle()).isNull();
    }

    private ChatRoom createChatRoom() {
        User user = userRepository.save(createUser());
        return chatRoomRepository.save(ChatRoom.builder().user(user).build());
    }

    private User createUser() {
        return User.createSocialUser(
                "test" + UUID.randomUUID() + "@test.com",
                "테스트 유저",
                "profile.png",
                "google"
        );
    }
}
