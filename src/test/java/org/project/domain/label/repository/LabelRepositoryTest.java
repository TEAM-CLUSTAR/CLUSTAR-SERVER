package org.project.domain.label.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.label.entity.Label;
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
@DisplayName("LabelRepository 테스트")
class LabelRepositoryTest {

    @Autowired
    private LabelRepository labelRepository;

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
            Label label = Label.create("parent-" + i, user);
            ReflectionTestUtils.setField(
                    label,
                    "createdAt",
                    LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(i)
            );
            labelRepository.save(label);
        }

        em.flush();
        em.clear();

        // when
        List<Label> result = labelRepository.findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(user.getId());

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

        Label parent = Label.create("parent", user);
        ReflectionTestUtils.setField(parent, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 0));
        labelRepository.save(parent);

        Label child1 = Label.create("child-1", user, parent);
        ReflectionTestUtils.setField(child1, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 10));
        labelRepository.save(child1);

        Label child2 = Label.create("child-2", user, parent);
        ReflectionTestUtils.setField(child2, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 20));
        labelRepository.save(child2);

        Label grand1 = Label.create("grand-1", user, child1);
        ReflectionTestUtils.setField(grand1, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 30));
        labelRepository.save(grand1);

        Label grand2 = Label.create("grand-2", user, child1);
        ReflectionTestUtils.setField(grand2, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 40));
        labelRepository.save(grand2);

        Label grand3 = Label.create("grand-3", user, child2);
        ReflectionTestUtils.setField(grand3, "createdAt", LocalDateTime.of(2026, 1, 1, 0, 50));
        labelRepository.save(grand3);

        em.flush();
        em.clear();

        // when
        List<Label> children = labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(user.getId(), parent.getId());
        List<Label> grandChildren = labelRepository.findByUserIdAndParentParentIdOrderByCreatedAtDesc(user.getId(), parent.getId());

        // then
        assertThat(children).extracting(Label::getName)
                .containsExactly("child-2", "child-1");
        assertThat(grandChildren).extracting(Label::getName)
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
