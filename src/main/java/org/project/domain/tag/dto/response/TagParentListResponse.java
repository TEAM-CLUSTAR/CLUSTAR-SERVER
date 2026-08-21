package org.project.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.tag.entity.Tag;

import java.util.List;

@Schema(requiredProperties = "tags")
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
