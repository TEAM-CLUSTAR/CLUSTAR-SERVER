package org.project.domain.tag.dto.response;

import org.project.domain.tag.entity.Tag;

import java.util.List;
import java.util.Map;

public record TagHierarchyResponse(
        TagSummaryResponse parentTag,
        List<TagTreeResponse> childTags
) {
    public static TagHierarchyResponse from(Tag parentTag, List<Tag> childTags, Map<Long, List<Tag>> grandChildTagsByParentId) {
        return new TagHierarchyResponse(
                TagSummaryResponse.from(parentTag),
                childTags.stream()
                        .map(child -> TagTreeResponse.from(
                                child,
                                grandChildTagsByParentId.getOrDefault(child.getId(), List.of())
                        ))
                        .toList()
        );
    }

    public record TagTreeResponse(
            Long tagId,
            String name,
            String color,
            Long parentId,
            List<TagTreeResponse> childTags
    ) {
        public static TagTreeResponse from(Tag tag, List<Tag> childTags) {
            return new TagTreeResponse(
                    tag.getId(),
                    tag.getName(),
                    tag.getColor(),
                    tag.getParent() == null ? null : tag.getParent().getId(),
                    childTags.stream()
                            .map(child -> new TagTreeResponse(
                                    child.getId(),
                                    child.getName(),
                                    child.getColor(),
                                    child.getParent() == null ? null : child.getParent().getId(),
                                    List.of()
                            ))
                            .toList()
            );
        }
    }
}
