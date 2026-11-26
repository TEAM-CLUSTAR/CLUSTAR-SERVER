package org.project.domain.review.service;

import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.EndReviewResponse;
import org.project.domain.review.dto.response.ReviewResponse;

public interface ReviewService {

    // AI가 본문을 분석해 빈틈 카드를 만들고 새 버전으로 저장한다
    ReviewResponse startReview(Long userId, Long memoId);

    // 메모의 현재 버전과 진행 중인 검토(카드·대화 포함)를 조회한다
    ActiveReviewResponse getActiveReview(Long userId, Long memoId);

    // 지금 버전을 종료로 남기고, 반영 안 한 카드는 옮기고 새 빈틈을 더해 다음 버전을 만든다
    ReviewResponse rerunReview(Long userId, Long reviewId);

    // 진행 중 검토를 종료한다(카드·대화·반영 결과는 기록으로 남는다)
    EndReviewResponse endReview(Long userId, Long reviewId);
}
