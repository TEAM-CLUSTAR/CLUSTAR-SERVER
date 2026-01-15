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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("MemoLabelRepository 테스트")
class MemoLabelRepositoryTest {

    @Autowired
    private MemoLabelRepository memoLabelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("memo 기준으로 MemoLabel이 모두 삭제된다")
    void deleteByMemo_success() {
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
        em.persist(memo);

        Label label1 = labelRepository.save(Label.create("라벨1", user));
        Label label2 = labelRepository.save(Label.create("라벨2", user));

        MemoLabel memoLabel1 = MemoLabel.create(memo, label1, 1);
        MemoLabel memoLabel2 = MemoLabel.create(memo, label2, 2);

        em.persist(memoLabel1);
        em.persist(memoLabel2);

        em.flush();
        em.clear();

        // when
        memoLabelRepository.deleteByMemo(memo);
        em.flush();
        em.clear();

        // then
        List<MemoLabel> result = em.getEntityManager()
                .createQuery("SELECT ml FROM MemoLabel ml", MemoLabel.class)
                .getResultList();

        assertThat(result).isEmpty();
    }
}
