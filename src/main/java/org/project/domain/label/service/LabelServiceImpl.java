package org.project.domain.label.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.label.dto.reponse.LabelListResponse;
import org.project.domain.label.entity.Label;
import org.project.domain.label.repository.LabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelServiceImpl implements LabelService{

    private final LabelRepository labelRepository;

    public LabelListResponse getAllLabels(Long userId) {
        List<Label> labels = labelRepository.findAllByUserId(userId);
        return LabelListResponse.from(labels);
    }
}
