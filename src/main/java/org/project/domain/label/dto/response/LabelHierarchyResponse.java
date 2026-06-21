package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;

import java.util.List;
import java.util.Map;

public record LabelHierarchyResponse(
        LabelSummaryResponse parentLabel,
        List<LabelTreeResponse> childLabels
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
            Long labelId,
            String name,
            List<LabelTreeResponse> childLabels
    ) {
        public static LabelTreeResponse from(Label label, List<Label> childLabels) {
            return new LabelTreeResponse(
                    label.getId(),
                    label.getName(),
                    childLabels.stream()
                            .map(child -> new LabelTreeResponse(
                                    child.getId(),
                                    child.getName(),
                                    List.of()
                            ))
                            .toList()
            );
        }
    }
}
