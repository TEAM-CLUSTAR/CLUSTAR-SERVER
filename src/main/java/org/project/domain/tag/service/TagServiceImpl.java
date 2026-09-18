package org.project.domain.tag.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.tag.dto.request.TagCreateRequest;
import org.project.domain.tag.dto.request.TagUpdateRequest;
import org.project.domain.tag.dto.response.TagHierarchyResponse;
import org.project.domain.tag.dto.response.TagListResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService{

    private static final int MAX_PARENT_TAG_COUNT = 10;

    private final TagRepository tagRepository;
    private final MemoTagRepository memoTagRepository;
    private final UserRepository userRepository;

    public TagListResponse getAllTags(Long userId) {
        List<Tag> tags = tagRepository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId);
        return TagListResponse.from(tags);
    }

    @Override
    public TagParentListResponse getParentTags(Long userId) {
        List<Tag> tags = new ArrayList<>(
                tagRepository.findTop10ByUserIdAndParentIsNullOrderByCreatedAtDescIdDesc(userId)
        );
        Collections.reverse(tags);
        return TagParentListResponse.from(tags);
    }

    @Override
    public TagHierarchyResponse getChildAndGrandChildTags(Long userId, Long parentTagId) {
        Tag parentTag = tagRepository.findByIdAndUserIdAndParentIsNull(parentTagId, userId)
                .orElseThrow(() -> new TagException(TagErrorCode.PARENT_TAG_NOT_FOUND));

        List<Tag> childTags = tagRepository.findByUserIdAndParentIdOrderByCreatedAtAscIdAsc(userId, parentTagId);
        List<Tag> grandChildTags = tagRepository.findByUserIdAndParentParentIdOrderByCreatedAtAscIdAsc(userId, parentTagId);

        Map<Long, List<Tag>> grandChildTagsByParentId = grandChildTags.stream()
                .collect(Collectors.groupingBy(tag -> tag.getParent().getId()));

        return TagHierarchyResponse.from(parentTag, childTags, grandChildTagsByParentId);
    }

    @Override
    @Transactional
    public TagSummaryResponse createTag(Long userId, TagCreateRequest request) {
        return createTagInternal(userId, request);
    }

    private TagSummaryResponse createTagInternal(Long userId, TagCreateRequest request) {
        Tag parentTag = null;

        if (request.parentTagId() != null) {
            validateParentTagId(request.parentTagId());
            parentTag = getTagOrThrow(userId, request.parentTagId());
            validateTagDepth(parentTag);
        }

        ensureTagNameIsUnique(userId, request.name(), null);

        User user = parentTag == null
                ? getUserForParentTagCreation(userId)
                : getUserOrThrow(userId);

        Tag tag = parentTag == null
                ? createParentTag(request.name(), userId, user)
                : Tag.create(request.name(), user, parentTag);

        Tag savedTag = tagRepository.save(tag);
        return TagSummaryResponse.from(savedTag);
    }

    @Override
    @Transactional
    public TagSummaryResponse updateTag(Long userId, Long tagId, TagUpdateRequest request) {
        Tag tag = getTagOrThrow(userId, tagId);
        ensureTagNameIsUnique(userId, request.name(), tagId);

        tag.rename(request.name());
        return TagSummaryResponse.from(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long userId, Long tagId) {
        Tag target = getTagOrThrow(userId, tagId);

        List<Tag> childTags = tagRepository.findByUserIdAndParentIdOrderByCreatedAtAscIdAsc(userId, tagId);
        List<Tag> grandChildTags = tagRepository.findByUserIdAndParentParentIdOrderByCreatedAtAscIdAsc(userId, tagId);

        List<Long> tagIds = new ArrayList<>();
        grandChildTags.forEach(tag -> tagIds.add(tag.getId()));
        childTags.forEach(tag -> tagIds.add(tag.getId()));
        tagIds.add(target.getId());

        if (!tagIds.isEmpty()) {
            memoTagRepository.deleteByTagIds(tagIds);
        }

        List<Tag> allTags = new ArrayList<>(grandChildTags);
        allTags.addAll(childTags);
        allTags.add(target);
        tagRepository.deleteAllInBatch(allTags);
    }

    private Tag getTagOrThrow(Long userId, Long tagId) {
        return tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new TagException(TagErrorCode.TAG_NOT_FOUND));
    }

    private void ensureTagNameIsUnique(Long userId, String name, Long currentTagId) {
        Optional<Tag> optionalTag = tagRepository.findByNameAndUserId(name, userId);

        if (optionalTag.isPresent() && !optionalTag.get().getId().equals(currentTagId)) {
            throw new TagException(TagErrorCode.TAG_ALREADY_EXISTS);
        }
    }

    private void validateTagDepth(Tag parentTag) {
        if (parentTag.getParent() != null && parentTag.getParent().getParent() != null) {
            throw new TagException(TagErrorCode.TAG_DEPTH_LIMIT_EXCEEDED);
        }
    }

    private void validateParentTagId(Long parentTagId) {
        if (parentTagId <= 0) {
            throw new TagException(TagErrorCode.INVALID_PARENT_TAG_ID);
        }
    }

    private Tag createParentTag(String name, Long userId, User user) {
        if (tagRepository.countByUserIdAndParentIsNull(userId) >= MAX_PARENT_TAG_COUNT) {
            throw new TagException(TagErrorCode.PARENT_TAG_LIMIT_EXCEEDED);
        }

        Set<String> usedColors = tagRepository.findAllByUserIdAndParentIsNull(userId).stream()
                .map(Tag::getColor)
                .collect(Collectors.toCollection(HashSet::new));

        return Tag.create(name, user, TagColorPalette.firstAvailableColor(usedColors));
    }

    private User getUserForParentTagCreation(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
    }
}
