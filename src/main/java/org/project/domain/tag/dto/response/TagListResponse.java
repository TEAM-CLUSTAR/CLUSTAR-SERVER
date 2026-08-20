package org.project.domain.tag.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.tag.entity.Tag;
import java.util.List;

@Schema(requiredProperties = "tags")
public record TagListResponse(
        List<TagResponse> tags
) {

    public static TagListResponse from(List<Tag> tagEntities) {
        return new TagListResponse(
                tagEntities.stream()
                        .map(TagResponse::from)
                        .toList()
        );
    }

    @Schema(requiredProperties = {"tagId", "name", "color", "parentId"})
    public record TagResponse(
            Long tagId,
            String name,
            String color,
            @Schema(nullable = true)
            Long parentId
    ) {
        public static TagResponse from(Tag tag) {
            return new TagResponse(
                    tag.getId(),
                    tag.getName(),
                    tag.getColor(),
                    tag.getParent() == null ? null : tag.getParent().getId()
            );
        }
    }
}
