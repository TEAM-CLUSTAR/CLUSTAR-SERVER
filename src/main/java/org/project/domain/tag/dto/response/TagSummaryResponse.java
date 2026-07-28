package org.project.domain.tag.dto.response;

import org.project.domain.tag.entity.Tag;

public record TagSummaryResponse(
        Long tagId,
        String name,
        String colorHex,
        Long parentId
) {
    public static TagSummaryResponse from(Tag tag) {
        return new TagSummaryResponse(
                tag.getId(),
                tag.getName(),
                tag.getColorHex(),
                tag.getParent() == null ? null : tag.getParent().getId()
        );
    }
}
