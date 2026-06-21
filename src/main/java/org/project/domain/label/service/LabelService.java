package org.project.domain.label.service;

import org.project.domain.label.dto.response.LabelListResponse;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;

public interface LabelService {

    LabelListResponse getAllLabels(Long userId);

    LabelParentListResponse getParentLabels(Long userId);

    LabelHierarchyResponse getChildAndGrandChildLabels(Long userId, Long parentLabelId);
}
