package org.project.domain.review.ai;

import lombok.RequiredArgsConstructor;
import org.project.domain.review.entity.ReviewAiCallLog;
import org.project.domain.review.entity.ReviewAiCallLog.ReviewAiCall;
import org.project.domain.review.repository.ReviewAiCallLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 호출 기록은 바깥 트랜잭션이 롤백돼도 남아야 하므로 항상 새 트랜잭션으로 저장한다
@Component
@RequiredArgsConstructor
public class ReviewAiCallLogWriter {

    private final ReviewAiCallLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(ReviewAiCall call, String responseText, long latencyMs) {
        repository.save(ReviewAiCallLog.success(call, responseText, latencyMs));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(ReviewAiCall call, String responseText, Exception e) {
        repository.save(ReviewAiCallLog.failure(call, responseText, e));
    }
}
