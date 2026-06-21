package org.project.domain.label.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.user.entity.User;
import org.project.global.exception.domainException.LabelException;
import org.project.global.exception.errorcode.LabelErrorCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("LabelService 테스트")
class LabelServiceTest {

    @InjectMocks
    private LabelServiceImpl labelService;

    @Mock
    private LabelRepository labelRepository;

    @Test
    @DisplayName("부모 태그 목록을 조회한다")
    void getParentLabels_success() {
        // given
        User user = createUser();
        Label parent1 = Label.create("parent-1", user);
        Label parent2 = Label.create("parent-2", user);

        when(labelRepository.findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(parent2, parent1));

        // when
        LabelParentListResponse response = labelService.getParentLabels(1L);

        // then
        assertThat(response.labels()).hasSize(2);
        assertThat(response.labels().get(0).name()).isEqualTo("parent-2");
    }

    @Test
    @DisplayName("부모 태그 기준 자식과 손자 태그를 트리 구조로 조회한다")
    void getChildAndGrandChildLabels_success() {
        // given
        User user = createUser();
        Label parent = Label.create("parent", user);
        Label child1 = Label.create("child-1", user, parent);
        Label child2 = Label.create("child-2", user, parent);
        Label grand1 = Label.create("grand-1", user, child1);
        Label grand2 = Label.create("grand-2", user, child2);

        ReflectionTestUtils.setField(parent, "id", 10L);
        ReflectionTestUtils.setField(child1, "id", 11L);
        ReflectionTestUtils.setField(child2, "id", 12L);
        ReflectionTestUtils.setField(grand1, "id", 21L);
        ReflectionTestUtils.setField(grand2, "id", 22L);

        when(labelRepository.findByIdAndUserIdAndParentIsNull(10L, 1L))
                .thenReturn(Optional.of(parent));
        when(labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(child2, child1));
        when(labelRepository.findByUserIdAndParentParentIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(grand2, grand1));

        // when
        LabelHierarchyResponse response = labelService.getChildAndGrandChildLabels(1L, 10L);

        // then
        assertThat(response.parentLabel().name()).isEqualTo("parent");
        assertThat(response.childLabels()).hasSize(2);
        assertThat(response.childLabels().get(0).name()).isEqualTo("child-2");
        assertThat(response.childLabels().get(0).childLabels()).extracting(LabelHierarchyResponse.LabelTreeResponse::name)
                .containsExactly("grand-2");
        assertThat(response.childLabels().get(1).childLabels()).extracting(LabelHierarchyResponse.LabelTreeResponse::name)
                .containsExactly("grand-1");
    }

    @Test
    @DisplayName("부모 태그가 없으면 예외를 던진다")
    void getChildAndGrandChildLabels_parentNotFound() {
        // given
        when(labelRepository.findByIdAndUserIdAndParentIsNull(10L, 1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> labelService.getChildAndGrandChildLabels(1L, 10L))
                .isInstanceOf(LabelException.class)
                .hasMessageContaining(LabelErrorCode.PARENT_LABEL_NOT_FOUND.getMsg());
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
