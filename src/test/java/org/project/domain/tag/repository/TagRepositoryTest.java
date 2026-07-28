package org.project.domain.tag.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.tag.entity.Tag;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("TagRepository 테스트")
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("부모 태그는 생성일 내림차순으로 최대 10개 조회된다")
    void findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc_success() {
        // given
        User user = userRepository.save(createUser());

        for (int i = 1; i <= 12; i++) {
            Tag tag = Tag.create("parent-" + i, user);
            ReflectionTestUtils.setField(
                    tag,
                    "createdAt",
                    LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(i)
            );
            tagRepository.save(tag);
        }

        em.flush();
        em.clear();

        // when
        List<Tag> result = tagRepository.findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(user.getId());

        // then
        assertThat(result).hasSize(10);
        assertThat(result.get(0).getName()).isEqualTo("parent-12");
        assertThat(result.get(9).getName()).isEqualTo("parent-3");
    }

    @Test
    @DisplayName("자식과 손자 태그를 부모 기준으로 조회할 수 있다")
    void findHierarchyByParent_success() {
        // given
        User user = userRepository.save(createUser());

        Tag parent = Tag.create("parent", user);
        ReflectionTestUtils.setField(parent, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        tagRepository.save(parent);

        Tag child1 = Tag.create("child-1", user, parent);
        ReflectionTestUtils.setField(child1, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 10));
        tagRepository.save(child1);

        Tag child2 = Tag.create("child-2", user, parent);
        ReflectionTestUtils.setField(child2, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 20));
        tagRepository.save(child2);

        Tag grand1 = Tag.create("grand-1", user, child1);
        ReflectionTestUtils.setField(grand1, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 30));
        tagRepository.save(grand1);

        Tag grand2 = Tag.create("grand-2", user, child1);
        ReflectionTestUtils.setField(grand2, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 40));
        tagRepository.save(grand2);

        Tag grand3 = Tag.create("grand-3", user, child2);
        ReflectionTestUtils.setField(grand3, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 50));
        tagRepository.save(grand3);

        em.flush();
        em.clear();

        // when
        List<Tag> children = tagRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(user.getId(), parent.getId());
        List<Tag> grandChildren = tagRepository.findByUserIdAndParentParentIdOrderByCreatedAtDesc(user.getId(), parent.getId());

        // then
        assertThat(children).extracting(Tag::getName)
                .containsExactly("child-2", "child-1");
        assertThat(grandChildren).extracting(Tag::getName)
                .containsExactly("grand-3", "grand-2", "grand-1");
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
