package org.project.domain.review.service;

import org.project.domain.review.dto.response.AnswerResponse;

// 빈틈 카드 하나에 대한 동작(답 보내기, 다듬기, 반영)
public interface ReviewFindingService {

    // 답을 저장하고, 첫 답이면 AI 후속 질문을 붙여 돌려준다
    AnswerResponse answer(Long userId, Long reviewId, Long findingId, String content);
}
