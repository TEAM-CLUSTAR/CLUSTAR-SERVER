package org.project.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewMessageRepository;
import org.project.domain.review.repository.ReviewRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    // 한 번도 검토하지 않은 원본 문서의 버전
    private static final int ORIGINAL_VERSION = 1;

    private final MemoRepository memoRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository findingRepository;
    private final ReviewMessageRepository messageRepository;

    @Override
    public ActiveReviewResponse getActiveReview(Long userId, Long memoId) {
        Memo memo = getMyMemo(userId, memoId);

        int currentVersion = reviewRepository.findLatestVersion(memoId).orElse(ORIGINAL_VERSION);
        ReviewResponse review = reviewRepository.findByMemoIdAndStatus(memoId, ReviewStatus.ACTIVE)
                .map(this::toResponse)
                .orElse(null);

        return new ActiveReviewResponse(memo.getId(), currentVersion, review);
    }

    // 카드는 번호순, 대화는 카드별로 묶어 붙인다(대화는 한 번에 조회)
    private ReviewResponse toResponse(Review review) {
        List<ReviewFinding> findings = findingRepository.findByReviewIdOrderByNumberAsc(review.getId());
        Map<Long, List<ReviewMessageResponse>> messagesByFinding = findMessagesByFinding(findings);

        List<FindingResponse> findingResponses = findings.stream()
                .map(finding -> FindingResponse.of(
                        finding,
                        messagesByFinding.getOrDefault(finding.getId(), List.of())
                ))
                .toList();

        return ReviewResponse.of(review, findingResponses);
    }

    private Map<Long, List<ReviewMessageResponse>> findMessagesByFinding(List<ReviewFinding> findings) {
        if (findings.isEmpty()) {
            return Map.of();
        }
        List<Long> findingIds = findings.stream().map(ReviewFinding::getId).toList();

        return messageRepository.findByFindingIdInOrderByIdAsc(findingIds).stream()
                .collect(Collectors.groupingBy(
                        message -> message.getFinding().getId(),
                        Collectors.mapping(ReviewMessageResponse::from, Collectors.toList())
                ));
    }

    // 휴지통 메모는 조회되지 않아 MEMO_NOT_FOUND로 응답한다(메모 상세 조회와 같은 규칙)
    private Memo getMyMemo(Long userId, Long memoId) {
        Memo memo = memoRepository.findByIdAndNotDeleted(memoId)
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));
        if (!memo.getUser().getId().equals(userId)) {
            throw new MemoException(MemoErrorCode.FORBIDDEN_MEMO);
        }
        return memo;
    }
}
