package org.project.domain.tag.dto.response;

import org.project.domain.tag.entity.Tag;
import java.util.List;

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

    public record TagResponse(
            Long tagId,
            String name,
            String backgroundColorHex,
            String textColorHex,
            Long parentId
    ) {
        public static TagResponse from(Tag tag) {
            return new TagResponse(
                    tag.getId(),
                    tag.getName(),
                    tag.getBackgroundColorHex(),
                    tag.getTextColorHex(),
                    tag.getParent() == null ? null : tag.getParent().getId()
            );
        }
    }
}
