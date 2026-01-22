package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;
import org.project.domain.memo.dto.response.MemoListDashboardResponse;

import java.util.List;

public record LabelListResponse(
        List<MemoListDashboardResponse.LabelResponse> labels
) {

    public static LabelListResponse from(List<Label> labelEntities) {
        return new LabelListResponse(
                labelEntities.stream()
                        .map(MemoListDashboardResponse.LabelResponse::from)
                        .toList()
        );
    }

    public record LabelResponse(
            Long labelId,
            String name
    ) {
        public static LabelResponse from(Label label) {
            return new LabelResponse(
                    label.getId(),
                    label.getName()
            );
        }
    }
}
