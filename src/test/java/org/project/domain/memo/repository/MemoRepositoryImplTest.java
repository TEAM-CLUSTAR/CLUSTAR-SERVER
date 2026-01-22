package org.project.domain.memo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("MemoRepositoryImpl 테스트")
class MemoRepositoryImplTest {

    @Autowired
    MemoRepository memoRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LabelRepository labelRepository;

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("라벨 조건 없이 최신 메모부터 조회된다")
    void findMemos_withoutLabel_success() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "test@test.com",
                        "유저",
                        "profile.png",
                        "google"
                )
        );

        Memo memo1 = Memo.createMemo("memo1", "content1", user);
        Memo memo2 = Memo.createMemo("memo2", "content2", user);

        em.persist(memo1);
        em.persist(memo2);
        em.flush();
        em.clear();

        // when
        List<Memo> result = memoRepository.findMemos(
                user.getId(),
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("memo2");
        assertThat(result.get(1).getTitle()).isEqualTo("memo1");
    }

    @Test
    @DisplayName("라벨로 메모 필터링된다")
    void findMemos_withLabel_success() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "label@test.com",
                        "유저",
                        "profile.png",
                        "google"
                )
        );

        Label label1 = labelRepository.save(Label.create("라벨1", user));
        Label label2 = labelRepository.save(Label.create("라벨2", user));

        Memo memo1 = Memo.createMemo("memo1", "content1", user);
        Memo memo2 = Memo.createMemo("memo2", "content2", user);

        em.persist(memo1);
        em.persist(memo2);

        em.persist(MemoLabel.create(memo1, label1, 1));
        em.persist(MemoLabel.create(memo2, label2, 1));

        em.flush();
        em.clear();

        // when
        List<Memo> result = memoRepository.findMemos(
                user.getId(),
                List.of(label1.getId()),
                null,
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("memo1");
    }

    @Test
    @DisplayName("커서 기반 페이지네이션이 정상 동작한다")
    void findMemos_cursorPaging_success() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "cursor@test.com",
                        "유저",
                        "profile.png",
                        "google"
                )
        );

        LocalDateTime base = LocalDateTime.now();

        Memo oldMemo = Memo.createMemo("old", "old", user);
        Memo midMemo = Memo.createMemo("mid", "mid", user);
        Memo newMemo = Memo.createMemo("new", "new", user);

        em.persist(oldMemo);
        em.persist(midMemo);
        em.persist(newMemo);
        em.flush();

        // createdAt 강제 수정
        em.getEntityManager().createQuery("""
                    update Memo m
                    set m.createdAt = :createdAt
                    where m.id = :id
                """)
                .setParameter("createdAt", base.minusMinutes(2))
                .setParameter("id", oldMemo.getId())
                .executeUpdate();

        em.getEntityManager().createQuery("""
                    update Memo m
                    set m.createdAt = :createdAt
                    where m.id = :id
                """)
                .setParameter("createdAt", base.minusMinutes(1))
                .setParameter("id", midMemo.getId())
                .executeUpdate();

        em.getEntityManager().createQuery("""
                    update Memo m
                    set m.createdAt = :createdAt
                    where m.id = :id
                """)
                .setParameter("createdAt", base)
                .setParameter("id", newMemo.getId())
                .executeUpdate();

        em.clear();

        // cursor = newMemo
        Memo cursor = memoRepository.findById(newMemo.getId()).get();

        // when
        List<Memo> result = memoRepository.findMemos(
                user.getId(),
                null,
                cursor.getCreatedAt(),
                cursor.getId(),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Memo::getTitle)
                .containsExactly("mid", "old");
    }

    @Test
    @DisplayName("삭제된 메모는 조회되지 않는다")
    void findMemos_deletedExcluded() {
        // given
        User user = userRepository.save(
                User.createSocialUser(
                        "delete@test.com",
                        "유저",
                        "profile.png",
                        "google"
                )
        );

        Memo memo = Memo.createMemo("삭제메모", "content", user);
        em.persist(memo);

        memo.delete(); // isDeleted = true
        em.flush();
        em.clear();

        // when
        List<Memo> result = memoRepository.findMemos(
                user.getId(),
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).isEmpty();
    }
}
