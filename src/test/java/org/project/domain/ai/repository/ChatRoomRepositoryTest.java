package org.project.domain.ai.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.ai.entity.ChatRoom;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("ChatRoomRepository 테스트")
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    /**
     * ChatRoom의 @Where(is_deleted = false) 때문에 삭제된 채팅방은 조회 자체가 되지 않는다.
     * 그래서 validateAccess는 삭제된 방에 대해 "이미 삭제됨"이 아니라 "존재하지 않음"으로 응답한다.
     * 이 동작이 깨지면 삭제된 방의 접근 응답이 바뀌므로 여기서 고정해 둔다.
     */
    @Test
    @DisplayName("soft delete된 채팅방은 findById로 조회되지 않는다")
    void findById_softDeleted_returnsEmpty() {
        // given
        ChatRoom chatRoom = chatRoomRepository.save(
                ChatRoom.builder().user(userRepository.save(createUser())).build()
        );
        Long chatRoomId = chatRoom.getId();

        chatRoom.markDeleted();
        em.flush();
        em.clear();

        // when & then
        assertThat(chatRoomRepository.findById(chatRoomId)).isEmpty();
    }

    @Test
    @DisplayName("활성 채팅방 조회는 삭제된 방을 건너뛰고 가장 최근 방을 반환한다")
    void findTopByUserIdOrderByIdDesc_skipsDeleted() {
        // given
        User user = userRepository.save(createUser());

        ChatRoom older = chatRoomRepository.save(ChatRoom.builder().user(user).build());
        ChatRoom latest = chatRoomRepository.save(ChatRoom.builder().user(user).build());

        latest.markDeleted();
        em.flush();
        em.clear();

        // when
        ChatRoom active = chatRoomRepository
                .findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow();

        // then
        assertThat(active.getId()).isEqualTo(older.getId());
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
