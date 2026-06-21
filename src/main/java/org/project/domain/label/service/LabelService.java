package org.project.domain.label.service;

import org.project.domain.label.dto.request.LabelCreateRequest;
import org.project.domain.label.dto.request.LabelUpdateRequest;
import org.project.domain.label.dto.response.LabelListResponse;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.dto.response.LabelSummaryResponse;

public interface LabelService {

    LabelListResponse getAllLabels(Long userId);

    LabelParentListResponse getParentLabels(Long userId);

    LabelHierarchyResponse getChildAndGrandChildLabels(Long userId, Long parentLabelId);

    LabelSummaryResponse createLabel(Long userId, LabelCreateRequest request);

    LabelSummaryResponse updateLabel(Long userId, Long labelId, LabelUpdateRequest request);

    void deleteLabel(Long userId, Long labelId);
}
