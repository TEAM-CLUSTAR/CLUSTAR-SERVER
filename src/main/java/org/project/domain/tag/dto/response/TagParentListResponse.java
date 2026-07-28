package org.project.domain.tag.dto.response;

import org.project.domain.tag.entity.Tag;

import java.util.List;

public record TagParentListResponse(
        List<TagSummaryResponse> tags
) {

    public static TagParentListResponse from(List<Tag> tagEntities) {
        return new TagParentListResponse(
                tagEntities.stream()
                        .map(TagSummaryResponse::from)
                        .toList()
        );
    }
}
