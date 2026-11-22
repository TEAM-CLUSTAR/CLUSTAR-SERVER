package org.project.domain.review.service;

import org.project.domain.review.dto.response.ActiveReviewResponse;

public interface ReviewService {

    // 메모의 현재 버전과 진행 중인 검토(카드·대화 포함)를 조회한다
    ActiveReviewResponse getActiveReview(Long userId, Long memoId);
}
