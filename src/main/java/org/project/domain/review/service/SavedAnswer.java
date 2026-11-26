package org.project.domain.review.service;

import org.project.domain.review.ai.dto.FollowUpInput;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.entity.FindingStatus;

// 답을 저장한 결과. 첫 답일 때만 followUpInput이 있다(그때만 AI 후속 질문을 만든다)
public record SavedAnswer(
        ReviewMessageResponse answer,
        FindingStatus status,
        FollowUpInput followUpInput
) {
}
