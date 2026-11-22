package org.project.domain.review.service;

import org.project.domain.review.dto.response.AnswerResponse;
import org.project.domain.review.dto.response.RefineResponse;

// 빈틈 카드 하나에 대한 동작(답 보내기, 다듬기, 반영)
public interface ReviewFindingService {

    // 답을 저장하고, 첫 답이면 AI 후속 질문을 붙여 돌려준다
    AnswerResponse answer(Long userId, Long reviewId, Long findingId, String content);

    // 저장된 답으로 문서 문장을 다듬어 미리보기용으로 돌려준다(DB에 저장하지 않음)
    RefineResponse refine(Long userId, Long reviewId, Long findingId);
}
