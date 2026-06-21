package org.project.domain.label.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.dto.request.LabelCreateRequest;
import org.project.domain.label.dto.request.LabelUpdateRequest;
import org.project.domain.label.dto.response.LabelHierarchyResponse;
import org.project.domain.label.dto.response.LabelListResponse;
import org.project.domain.label.dto.response.LabelParentListResponse;
import org.project.domain.label.dto.response.LabelSummaryResponse;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.project.domain.memo.repository.MemoLabelRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.exception.domainException.LabelException;
import org.project.global.exception.errorcode.LabelErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelServiceImpl implements LabelService{

    private final LabelRepository labelRepository;
    private final MemoLabelRepository memoLabelRepository;
    private final UserRepository userRepository;

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

    @Override
    @Transactional
    public LabelSummaryResponse createLabel(Long userId, LabelCreateRequest request) {
        return createLabelInternal(userId, request);
    }

    private LabelSummaryResponse createLabelInternal(Long userId, LabelCreateRequest request) {
        Label parentLabel = null;

        if (request.parentLabelId() != null) {
            parentLabel = getLabelOrThrow(userId, request.parentLabelId());
            validateLabelDepth(parentLabel);
        }

        ensureLabelNameIsUnique(userId, request.name(), null);

        User user = getUserOrThrow(userId);

        Label label = parentLabel == null
                ? Label.create(request.name(), user)
                : Label.create(request.name(), user, parentLabel);

        Label savedLabel = labelRepository.save(label);
        return LabelSummaryResponse.from(savedLabel);
    }

    @Override
    @Transactional
    public LabelSummaryResponse updateLabel(Long userId, Long labelId, LabelUpdateRequest request) {
        Label label = getLabelOrThrow(userId, labelId);
        ensureLabelNameIsUnique(userId, request.name(), labelId);

        label.rename(request.name());
        return LabelSummaryResponse.from(label);
    }

    @Override
    @Transactional
    public void deleteLabel(Long userId, Long labelId) {
        Label target = getLabelOrThrow(userId, labelId);

        List<Label> childLabels = labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(userId, labelId);
        List<Label> grandChildLabels = new ArrayList<>();
        for (Label child : childLabels) {
            grandChildLabels.addAll(labelRepository.findByUserIdAndParentIdOrderByCreatedAtDesc(userId, child.getId()));
        }

        List<Long> labelIds = new ArrayList<>();
        grandChildLabels.forEach(label -> labelIds.add(label.getId()));
        childLabels.forEach(label -> labelIds.add(label.getId()));
        labelIds.add(target.getId());

        if (!labelIds.isEmpty()) {
            memoLabelRepository.deleteByLabelIds(labelIds);
        }

        grandChildLabels.forEach(labelRepository::delete);
        childLabels.forEach(labelRepository::delete);
        labelRepository.delete(target);
    }

    private Label getLabelOrThrow(Long userId, Long labelId) {
        return labelRepository.findByIdAndUserId(labelId, userId)
                .orElseThrow(() -> new LabelException(LabelErrorCode.LABEL_NOT_FOUND));
    }

    private void ensureLabelNameIsUnique(Long userId, String name, Long currentLabelId) {
        Optional<Label> optionalLabel = labelRepository.findByNameAndUserId(name, userId);

        if (optionalLabel.isPresent() && !optionalLabel.get().getId().equals(currentLabelId)) {
            throw new LabelException(LabelErrorCode.LABEL_ALREADY_EXISTS);
        }
    }

    private void validateLabelDepth(Label parentLabel) {
        if (parentLabel.getParent() != null && parentLabel.getParent().getParent() != null) {
            throw new LabelException(LabelErrorCode.LABEL_DEPTH_LIMIT_EXCEEDED);
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new LabelException(LabelErrorCode.LABEL_NOT_FOUND));
    }
}
