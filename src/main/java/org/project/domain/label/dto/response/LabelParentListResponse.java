package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;

import java.util.List;

public record LabelParentListResponse(
        List<LabelSummaryResponse> labels
) {

    public static LabelParentListResponse from(List<Label> labelEntities) {
        return new LabelParentListResponse(
                labelEntities.stream()
                        .map(LabelSummaryResponse::from)
                        .toList()
        );
    }
}
