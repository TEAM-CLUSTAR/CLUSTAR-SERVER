package org.project.domain.label.service;

import org.project.domain.label.dto.response.LabelListResponse;

public interface LabelService {

    LabelListResponse getAllLabels(Long userId);
}
