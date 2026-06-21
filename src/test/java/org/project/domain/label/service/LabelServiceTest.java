package org.project.domain.label.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.label.dto.request.LabelCreateRequest;
import org.project.domain.label.dto.request.LabelUpdateRequest;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.dto.response.LabelSummaryResponse;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.repository.MemoLabelRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.LabelException;
import org.project.global.exception.errorcode.LabelErrorCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LabelService 테스트")
class LabelServiceTest {

    private static final List<String> COLOR_PALETTE = List.of(
            "#ABDEE6", "#CBAACB", "#FFFFB5", "#FFCCB6", "#F3B0C3",
            "#C6DBDA", "#FEE1E8", "#FED7C3", "#F6EAC2", "#ECD5E3",
            "#FF968A", "#FFAEA5", "#FFC5BF", "#FFD8BE", "#FFC8A2",
            "#D4F0F0", "#8FCACA", "#CCE2CB", "#B6CFB6", "#97C1A9",
            "#FCB9AA", "#FFDBCC", "#ECEAE4", "#A2E1DB", "#55CBCD"
    );

    @InjectMocks
    private LabelServiceImpl labelService;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private MemoLabelRepository memoLabelRepository;

    @Mock
    private UserRepository userRepository;

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
        assertThat(response.labels().get(0).colorHex()).isIn(COLOR_PALETTE);
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
        assertThat(response.parentLabel().colorHex()).isIn(COLOR_PALETTE);
        assertThat(response.childLabels()).hasSize(2);
        assertThat(response.childLabels().get(0).name()).isEqualTo("child-2");
        assertThat(response.childLabels().get(0).colorHex()).isIn(COLOR_PALETTE);
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

    @Test
    @DisplayName("부모 태그를 생성한다")
    void createLabel_parent_success() {
        // given
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(labelRepository.findByNameAndUserId("parent", 1L)).thenReturn(Optional.empty());
        Label saved = Label.create("parent", user);
        ReflectionTestUtils.setField(saved, "id", 100L);
        when(labelRepository.save(any(Label.class))).thenReturn(saved);

        // when
        LabelSummaryResponse response = labelService.createLabel(1L, new LabelCreateRequest("parent", null));

        // then
        assertThat(response.labelId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("parent");
        assertThat(response.colorHex()).isIn(COLOR_PALETTE);
    }

    @Test
    @DisplayName("자식 태그를 생성한다")
    void createLabel_child_success() {
        // given
        User user = createUser();
        Label parent = Label.create("parent", user);
        ReflectionTestUtils.setField(parent, "id", 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(labelRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(parent));
        when(labelRepository.findByNameAndUserId("child", 1L)).thenReturn(Optional.empty());

        Label saved = Label.create("child", user, parent);
        ReflectionTestUtils.setField(saved, "id", 101L);
        when(labelRepository.save(any(Label.class))).thenReturn(saved);

        // when
        LabelSummaryResponse response = labelService.createLabel(1L, new LabelCreateRequest("child", 10L));

        // then
        assertThat(response.labelId()).isEqualTo(101L);
        assertThat(response.name()).isEqualTo("child");
        assertThat(response.colorHex()).isIn(COLOR_PALETTE);
    }

    @Test
    @DisplayName("태그 이름을 수정한다")
    void updateLabel_success() {
        // given
        User user = createUser();
        Label label = Label.create("old", user);
        ReflectionTestUtils.setField(label, "id", 10L);

        when(labelRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(label));
        when(labelRepository.findByNameAndUserId("new", 1L)).thenReturn(Optional.empty());

        // when
        LabelSummaryResponse response = labelService.updateLabel(1L, 10L, new LabelUpdateRequest("new"));

        // then
        assertThat(response.labelId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("new");
        assertThat(response.colorHex()).isIn(COLOR_PALETTE);
    }

    @Test
    @DisplayName("태그를 삭제하면 연관 메모 태그도 삭제한다")
    void deleteLabel_success() {
        // given
        User user = createUser();
        Label parent = Label.create("parent", user);
        ReflectionTestUtils.setField(parent, "id", 10L);

        Label child = Label.create("child", user, parent);
        ReflectionTestUtils.setField(child, "id", 11L);

        when(labelRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(parent));
        when(labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(child));
        when(labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(1L, 11L))
                .thenReturn(List.of());

        // when
        labelService.deleteLabel(1L, 10L);

        // then
        verify(memoLabelRepository).deleteByLabelIds(List.of(11L, 10L));
        verify(labelRepository).delete(child);
        verify(labelRepository).delete(parent);
    }

    @Test
    @DisplayName("중복된 태그 이름이면 예외를 던진다")
    void createLabel_duplicateName_fail() {
        // given
        User user = createUser();
        Label existing = Label.create("parent", user);
        ReflectionTestUtils.setField(existing, "id", 10L);

        when(labelRepository.findByNameAndUserId("parent", 1L)).thenReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> labelService.createLabel(1L, new LabelCreateRequest("parent", null)))
                .isInstanceOf(LabelException.class)
                .hasMessageContaining(LabelErrorCode.LABEL_ALREADY_EXISTS.getMsg());
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
