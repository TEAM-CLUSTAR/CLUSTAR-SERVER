package org.project.domain.tag.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.tag.dto.request.TagCreateRequest;
import org.project.domain.tag.dto.request.TagUpdateRequest;
import org.project.domain.tag.dto.response.TagHierarchyResponse;
import org.project.domain.tag.dto.response.TagParentListResponse;
import org.project.domain.tag.dto.response.TagSummaryResponse;
import org.project.domain.tag.entity.Tag;
import org.project.domain.tag.repository.TagRepository;
import org.project.domain.tag.util.TagColorPalette;
import org.project.domain.memo.repository.MemoTagRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.TagException;
import org.project.global.exception.domainException.UserException;
import org.project.global.exception.errorcode.TagErrorCode;
import org.project.global.exception.errorcode.UserErrorCode;

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
@DisplayName("TagService 테스트")
class TagServiceTest {

    @InjectMocks
    private TagServiceImpl tagService;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private MemoTagRepository memoTagRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("부모 태그 목록을 조회한다")
    void getParentTags_success() {
        // given
        User user = createUser();
        Tag parent1 = Tag.create("parent-1", user);
        Tag parent2 = Tag.create("parent-2", user);

        when(tagRepository.findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(parent2, parent1));

        // when
        TagParentListResponse response = tagService.getParentTags(1L);

        // then
        assertThat(response.tags()).hasSize(2);
        assertThat(response.tags().get(0).name()).isEqualTo("parent-2");
        assertThat(response.tags().get(0).backgroundColorHex()).isIn(TagColorPalette.backgroundColors());
        assertThat(response.tags().get(0).textColorHex()).isIn(TagColorPalette.textColors());
        assertThat(response.tags().get(0).parentId()).isNull();
    }

    @Test
    @DisplayName("부모 태그 기준 자식과 손자 태그를 트리 구조로 조회한다")
    void getChildAndGrandChildTags_success() {
        // given
        User user = createUser();
        Tag parent = Tag.create("parent", user);
        Tag child1 = Tag.create("child-1", user, parent);
        Tag child2 = Tag.create("child-2", user, parent);
        Tag grand1 = Tag.create("grand-1", user, child1);
        Tag grand2 = Tag.create("grand-2", user, child2);

        ReflectionTestUtils.setField(parent, "id", 10L);
        ReflectionTestUtils.setField(child1, "id", 11L);
        ReflectionTestUtils.setField(child2, "id", 12L);
        ReflectionTestUtils.setField(grand1, "id", 21L);
        ReflectionTestUtils.setField(grand2, "id", 22L);

        when(tagRepository.findByIdAndUserIdAndParentIsNull(10L, 1L))
                .thenReturn(Optional.of(parent));
        when(tagRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(child2, child1));
        when(tagRepository.findByUserIdAndParentParentIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(grand2, grand1));

        // when
        TagHierarchyResponse response = tagService.getChildAndGrandChildTags(1L, 10L);

        // then
        assertThat(response.parentTag().name()).isEqualTo("parent");
        assertThat(response.parentTag().backgroundColorHex()).isIn(TagColorPalette.backgroundColors());
        assertThat(response.parentTag().textColorHex()).isIn(TagColorPalette.textColors());
        assertThat(response.parentTag().parentId()).isNull();
        assertThat(response.childTags()).hasSize(2);
        assertThat(response.childTags().get(0).name()).isEqualTo("child-2");
        assertThat(response.childTags().get(0).backgroundColorHex()).isEqualTo(response.parentTag().backgroundColorHex());
        assertThat(response.childTags().get(0).textColorHex()).isEqualTo(response.parentTag().textColorHex());
        assertThat(response.childTags().get(0).parentId()).isEqualTo(10L);
        assertThat(response.childTags().get(0).childTags()).extracting(TagHierarchyResponse.TagTreeResponse::name)
                .containsExactly("grand-2");
        assertThat(response.childTags().get(0).childTags().get(0).parentId()).isEqualTo(12L);
        assertThat(response.childTags().get(0).childTags().get(0).backgroundColorHex())
                .isEqualTo(response.parentTag().backgroundColorHex());
        assertThat(response.childTags().get(0).childTags().get(0).textColorHex())
                .isEqualTo(response.parentTag().textColorHex());
        assertThat(response.childTags().get(1).childTags()).extracting(TagHierarchyResponse.TagTreeResponse::name)
                .containsExactly("grand-1");
    }

    @Test
    @DisplayName("부모 태그가 없으면 예외를 던진다")
    void getChildAndGrandChildTags_parentNotFound() {
        // given
        when(tagRepository.findByIdAndUserIdAndParentIsNull(10L, 1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tagService.getChildAndGrandChildTags(1L, 10L))
                .isInstanceOf(TagException.class)
                .hasMessageContaining(TagErrorCode.PARENT_TAG_NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("부모 태그를 생성한다")
    void createTag_parent_success() {
        // given
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tagRepository.findByNameAndUserId("parent", 1L)).thenReturn(Optional.empty());
        Tag saved = Tag.create("parent", user);
        ReflectionTestUtils.setField(saved, "id", 100L);
        when(tagRepository.save(any(Tag.class))).thenReturn(saved);

        // when
        TagSummaryResponse response = tagService.createTag(1L, new TagCreateRequest("parent", null));

        // then
        assertThat(response.tagId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("parent");
        assertThat(response.backgroundColorHex()).isIn(TagColorPalette.backgroundColors());
        assertThat(response.textColorHex()).isIn(TagColorPalette.textColors());
        assertThat(response.parentId()).isNull();
    }

    @Test
    @DisplayName("자식 태그를 생성한다")
    void createTag_child_success() {
        // given
        User user = createUser();
        Tag parent = Tag.create("parent", user);
        ReflectionTestUtils.setField(parent, "id", 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tagRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(parent));
        when(tagRepository.findByNameAndUserId("child", 1L)).thenReturn(Optional.empty());

        Tag saved = Tag.create("child", user, parent);
        ReflectionTestUtils.setField(saved, "id", 101L);
        when(tagRepository.save(any(Tag.class))).thenReturn(saved);

        // when
        TagSummaryResponse response = tagService.createTag(1L, new TagCreateRequest("child", 10L));

        // then
        assertThat(response.tagId()).isEqualTo(101L);
        assertThat(response.name()).isEqualTo("child");
        assertThat(response.backgroundColorHex()).isEqualTo(parent.getBackgroundColorHex());
        assertThat(response.textColorHex()).isEqualTo(parent.getTextColorHex());
        assertThat(response.parentId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("태그 이름을 수정한다")
    void updateTag_success() {
        // given
        User user = createUser();
        Tag tag = Tag.create("old", user);
        ReflectionTestUtils.setField(tag, "id", 10L);

        when(tagRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(tag));
        when(tagRepository.findByNameAndUserId("new", 1L)).thenReturn(Optional.empty());

        // when
        TagSummaryResponse response = tagService.updateTag(1L, 10L, new TagUpdateRequest("new"));

        // then
        assertThat(response.tagId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("new");
        assertThat(response.backgroundColorHex()).isIn(TagColorPalette.backgroundColors());
        assertThat(response.textColorHex()).isIn(TagColorPalette.textColors());
        assertThat(response.parentId()).isNull();
    }

    @Test
    @DisplayName("태그를 삭제하면 연관 메모 태그도 삭제한다")
    void deleteTag_success() {
        // given
        User user = createUser();
        Tag parent = Tag.create("parent", user);
        ReflectionTestUtils.setField(parent, "id", 10L);

        Tag child = Tag.create("child", user, parent);
        ReflectionTestUtils.setField(child, "id", 11L);

        when(tagRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(parent));
        when(tagRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(child));
        when(tagRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(1L, 11L))
                .thenReturn(List.of());

        // when
        tagService.deleteTag(1L, 10L);

        // then
        verify(memoTagRepository).deleteByTagIds(List.of(11L, 10L));
        verify(tagRepository).delete(child);
        verify(tagRepository).delete(parent);
    }

    @Test
    @DisplayName("중복된 태그 이름이면 예외를 던진다")
    void createTag_duplicateName_fail() {
        // given
        User user = createUser();
        Tag existing = Tag.create("parent", user);
        ReflectionTestUtils.setField(existing, "id", 10L);

        when(tagRepository.findByNameAndUserId("parent", 1L)).thenReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> tagService.createTag(1L, new TagCreateRequest("parent", null)))
                .isInstanceOf(TagException.class)
                .hasMessageContaining(TagErrorCode.TAG_ALREADY_EXISTS.getMsg());
    }

    @Test
    @DisplayName("사용자가 없으면 사용자 예외를 던진다")
    void createTag_userNotFound_fail() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tagService.createTag(1L, new TagCreateRequest("parent", null)))
                .isInstanceOf(UserException.class)
                .hasMessageContaining(UserErrorCode.NOT_FOUND_USER.getMsg());
    }

    @Test
    @DisplayName("부모 태그 ID가 0 이하이면 예외를 던진다")
    void createTag_invalidParentId_fail() {
        // when & then
        assertThatThrownBy(() -> tagService.createTag(1L, new TagCreateRequest("child", 0L)))
                .isInstanceOf(TagException.class)
                .hasMessageContaining(TagErrorCode.INVALID_PARENT_TAG_ID.getMsg());
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
