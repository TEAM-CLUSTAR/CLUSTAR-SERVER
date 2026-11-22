package org.project.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewRepository;
import org.project.domain.review.support.ValidFindingDraft;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 검토 DB 쓰기만 모아 둔 곳.
// AI 호출(수 초)은 서비스에서 트랜잭션 밖에서 하고, 저장만 여기서 짧은 트랜잭션으로 처리한다
@Component
@RequiredArgsConstructor
@Transactional
public class ReviewWriter {

    // 원본 문서가 v1이므로 첫 검토는 v2
    private static final int ORIGINAL_VERSION = 1;

    private final MemoRepository memoRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository findingRepository;

    public ReviewResponse saveNewReview(Long memoId, String content, List<ValidFindingDraft> drafts) {
        // 서비스에서 확인한 뒤 AI가 분석하는 사이 다른 요청이 먼저 저장했을 수 있어 한 번 더 확인한다
        if (reviewRepository.findByMemoIdAndStatus(memoId, ReviewStatus.ACTIVE).isPresent()) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
        }

        int version = reviewRepository.findLatestVersion(memoId).orElse(ORIGINAL_VERSION) + 1;
        Review review = reviewRepository.save(
                Review.start(memoRepository.getReferenceById(memoId), version, content));

        List<ReviewFinding> findings = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            ValidFindingDraft draft = drafts.get(i);
            findings.add(ReviewFinding.create(
                    review,
                    i + 1,
                    draft.category(),
                    draft.sectionLabel(),
                    draft.anchorText(),
                    draft.title(),
                    draft.reason(),
                    draft.evidence(),
                    draft.question()
            ));
        }
        findingRepository.saveAll(findings);

        return ReviewResponse.of(review, findings.stream()
                .map(finding -> FindingResponse.of(finding, List.of()))
                .toList());
    }
}
