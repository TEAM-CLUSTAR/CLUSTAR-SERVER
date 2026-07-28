package org.project.domain.tag.service;

import org.project.domain.tag.dto.request.TagCreateRequest;
import org.project.domain.tag.dto.request.TagUpdateRequest;
import org.project.domain.tag.dto.response.TagListResponse;
import org.project.domain.tag.dto.response.TagHierarchyResponse;
import org.project.domain.tag.dto.response.TagParentListResponse;
import org.project.domain.tag.dto.response.TagSummaryResponse;

public interface TagService {

    TagListResponse getAllTags(Long userId);

    TagParentListResponse getParentTags(Long userId);

    TagHierarchyResponse getChildAndGrandChildTags(Long userId, Long parentTagId);

    TagSummaryResponse createTag(Long userId, TagCreateRequest request);

    TagSummaryResponse updateTag(Long userId, Long tagId, TagUpdateRequest request);

    void deleteTag(Long userId, Long tagId);
}
