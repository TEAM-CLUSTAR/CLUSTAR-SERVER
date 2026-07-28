package org.project.domain.memo.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.tag.entity.Tag;
import org.project.domain.tag.repository.TagRepository;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
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
@DisplayName("MemoTagRepository 테스트")
class MemoTagRepositoryTest {

    @Autowired
    private MemoTagRepository memoTagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("memo 기준으로 MemoTag가 모두 삭제된다")
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

        Tag tag1 = tagRepository.save(Tag.create("태그1", user));
        Tag tag2 = tagRepository.save(Tag.create("태그2", user));

        MemoTag memoTag1 = MemoTag.create(memo, tag1, 1);
        MemoTag memoTag2 = MemoTag.create(memo, tag2, 2);

        em.persist(memoTag1);
        em.persist(memoTag2);

        em.flush();
        em.clear();

        // when
        memoTagRepository.deleteByMemo(memo);
        em.flush();
        em.clear();

        // then
        List<MemoTag> result = em.getEntityManager()
                .createQuery("SELECT mt FROM MemoTag mt", MemoTag.class)
                .getResultList();

        assertThat(result).isEmpty();
    }
}
