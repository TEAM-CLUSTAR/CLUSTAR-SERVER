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
    void deleteByMemoIdIn_success() {
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
        memoTagRepository.deleteByMemoIdIn(List.of(memo.getId()));
        em.flush();
        em.clear();

        // then
        List<MemoTag> result = em.getEntityManager()
                .createQuery("SELECT mt FROM MemoTag mt", MemoTag.class)
                .getResultList();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteByMemoIdIn은 대상 메모들의 태그 연결만 지우고 다른 메모 연결은 남긴다")
    void deleteByMemoIdIn_keepsNonTarget() {
        User user = userRepository.save(User.createSocialUser(
                "batch" + UUID.randomUUID() + "@test.com", "테스트 유저", "profile.png", "google"));
        Memo target1 = Memo.createMemo("대상1", "내용", user);
        Memo target2 = Memo.createMemo("대상2", "내용", user);
        Memo other = Memo.createMemo("비대상", "내용", user);
        em.persist(target1);
        em.persist(target2);
        em.persist(other);
        Tag tag = tagRepository.save(Tag.create("공통태그", user));
        for (Memo m : List.of(target1, target2, other)) {
            em.persist(MemoTag.create(m, tag, 1));
        }
        em.flush();
        em.clear();

        memoTagRepository.deleteByMemoIdIn(List.of(target1.getId(), target2.getId()));
        em.flush();
        em.clear();

        List<MemoTag> result = em.getEntityManager()
                .createQuery("SELECT mt FROM MemoTag mt", MemoTag.class)
                .getResultList();
        assertThat(result).extracting(memoTag -> memoTag.getMemo().getId())
                .containsExactly(other.getId());
    }
}
