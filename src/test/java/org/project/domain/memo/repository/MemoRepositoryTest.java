package org.project.domain.memo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
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
@DisplayName("MemoRepository 테스트")
@Import(QuerydslTestConfig.class)
class MemoRepositoryTest {

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;


    @Test
    @DisplayName("삭제되지 않은 메모는 조회된다")
    void find_notDeleted_memo_success() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "test" + UUID.randomUUID() + "@test.com",
                        "테스트 유저",
                        "profile.png",
                        "google"
                )
        );

        Memo memo = Memo.createMemo("제목", "내용", user);
        memoRepository.save(memo);

        em.flush();
        em.clear();

        // when
        var result = memoRepository.findByIdAndNotDeleted(memo.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("삭제된 메모는 조회되지 않는다")
    void find_deleted_memo_fail() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "test" + UUID.randomUUID() + "@test.com",
                        "테스트 유저",
                        "profile.png",
                        "google"
                )
        );

        Memo memo = Memo.createMemo("제목", "내용", user);
        memoRepository.save(memo);

        memo.delete(); // isDeleted = true
        em.flush();
        em.clear();

        // when
        var result = memoRepository.findByIdAndNotDeleted(memo.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 memoId는 조회되지 않는다")
    void find_notExist_memo_fail() {
        // when
        var result = memoRepository.findByIdAndNotDeleted(999L);

        // then
        assertThat(result).isEmpty();
    }
}
