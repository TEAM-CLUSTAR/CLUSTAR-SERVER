package org.project.domain.label.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelListResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.global.exception.domainException.LabelException;
import org.project.global.exception.errorcode.LabelErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelServiceImpl implements LabelService{

    private final LabelRepository labelRepository;

    public LabelListResponse getAllLabels(Long userId) {
        List<Label> labels = labelRepository.findAllByUserId(userId);
        return LabelListResponse.from(labels);
    }

    @Override
    public LabelParentListResponse getParentLabels(Long userId) {
        List<Label> labels = labelRepository.findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(userId);
        return LabelParentListResponse.from(labels);
    }

    @Override
    public LabelHierarchyResponse getChildAndGrandChildLabels(Long userId, Long parentLabelId) {
        Label parentLabel = labelRepository.findByIdAndUserIdAndParentIsNull(parentLabelId, userId)
                .orElseThrow(() -> new LabelException(LabelErrorCode.PARENT_LABEL_NOT_FOUND));

        List<Label> childLabels = labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(userId, parentLabelId);
        List<Label> grandChildLabels = labelRepository.findByUserIdAndParentParentIdOrderByCreatedAtDesc(userId, parentLabelId);

        Map<Long, List<Label>> grandChildLabelsByParentId = grandChildLabels.stream()
                .collect(Collectors.groupingBy(label -> label.getParent().getId()));

        return LabelHierarchyResponse.from(parentLabel, childLabels, grandChildLabelsByParentId);
    }
}
