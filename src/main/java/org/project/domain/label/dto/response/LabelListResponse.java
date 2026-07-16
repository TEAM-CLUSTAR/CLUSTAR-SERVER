package org.project.domain.label.dto.response;

import org.project.domain.label.entity.Label;
import java.util.List;

public record LabelListResponse(
        List<LabelResponse> tags
) {

    public static LabelListResponse from(List<Label> labelEntities) {
        return new LabelListResponse(
                labelEntities.stream()
                        .map(LabelResponse::from)
                        .toList()
        );
    }

    public record LabelResponse(
            Long tagId,
            String name,
            String colorHex,
            Long parentId
    ) {
        public static LabelResponse from(Label label) {
            return new LabelResponse(
                    label.getId(),
                    label.getName(),
                    label.getColorHex(),
                    label.getParent() == null ? null : label.getParent().getId()
            );
        }
    }
}
