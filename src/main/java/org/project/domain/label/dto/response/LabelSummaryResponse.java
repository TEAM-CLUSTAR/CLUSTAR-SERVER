package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;

public record LabelSummaryResponse(
        Long labelId,
        String name,
        String colorHex
) {
    public static LabelSummaryResponse from(Label label) {
        return new LabelSummaryResponse(
                label.getId(),
                label.getName(),
                label.getColorHex()
        );
    }
}
