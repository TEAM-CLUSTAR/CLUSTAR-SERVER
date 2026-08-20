package org.project.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.tag.entity.Tag;

@Schema(requiredProperties = {"tagId", "name", "color", "parentId"})
public record TagSummaryResponse(
        Long tagId,
        String name,
        String color,
        @Schema(nullable = true)
        Long parentId
) {
    public static TagSummaryResponse from(Tag tag) {
        return new TagSummaryResponse(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getParent() == null ? null : tag.getParent().getId()
        );
    }
}
