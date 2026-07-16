package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;

import java.util.List;
import java.util.Map;

public record LabelHierarchyResponse(
        LabelSummaryResponse parentTag,
        List<LabelTreeResponse> childTags
) {
    public static LabelHierarchyResponse from(Label parentLabel, List<Label> childLabels, Map<Long, List<Label>> grandChildLabelsByParentId) {
        return new LabelHierarchyResponse(
                LabelSummaryResponse.from(parentLabel),
                childLabels.stream()
                        .map(child -> LabelTreeResponse.from(
                                child,
                                grandChildLabelsByParentId.getOrDefault(child.getId(), List.of())
                        ))
                        .toList()
        );
    }

    public record LabelTreeResponse(
            Long tagId,
            String name,
            String colorHex,
            Long parentId,
            List<LabelTreeResponse> childTags
    ) {
        public static LabelTreeResponse from(Label label, List<Label> childLabels) {
            return new LabelTreeResponse(
                    label.getId(),
                    label.getName(),
                    label.getColorHex(),
                    label.getParent() == null ? null : label.getParent().getId(),
                    childLabels.stream()
                            .map(child -> new LabelTreeResponse(
                                    child.getId(),
                                    child.getName(),
                                    child.getColorHex(),
                                    child.getParent() == null ? null : child.getParent().getId(),
                                    List.of()
                            ))
                            .toList()
            );
        }
    }
}
