package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;

public record LabelSummaryResponse(
        Long tagId,
        String name,
        String colorHex,
        Long parentId
) {
    public static LabelSummaryResponse from(Label label) {
        return new LabelSummaryResponse(
                label.getId(),
                label.getName(),
                label.getColorHex(),
                label.getParent() == null ? null : label.getParent().getId()
        );
    }
}
