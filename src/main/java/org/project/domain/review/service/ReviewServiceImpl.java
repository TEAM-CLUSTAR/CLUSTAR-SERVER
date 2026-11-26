package org.project.domain.review.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.ai.ReviewAiClient;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.ai.dto.ResolvedGap;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.EndReviewResponse;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewMessageRepository;
import org.project.domain.review.repository.ReviewRepository;
import org.project.domain.review.support.FindingDrafts;
import org.project.domain.review.support.ValidFindingDraft;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.domainException.ReviewLengthException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final String VERSION_UNIQUE_CONSTRAINT = "uk_review_memo_version";

    // 한 번도 검토하지 않은 원본 문서의 버전
    private static final int ORIGINAL_VERSION = 1;

    private final MemoRepository memoRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository findingRepository;
    private final ReviewMessageRepository messageRepository;
    private final ReviewAiClient reviewAiClient;
    private final ReviewWriter reviewWriter;
    private final ReviewProperties reviewProperties;

    // AI 분석(수 초)이 끝날 때까지 DB 커넥션을 잡고 있지 않도록 트랜잭션 밖에서 실행하고,
    // 저장만 ReviewWriter의 짧은 트랜잭션으로 처리한다
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReviewResponse startReview(Long userId, Long memoId) {
        Memo memo = getMyMemo(userId, memoId);
        String content = memo.getContent();
        validateContentLength(content);
        if (reviewRepository.findByMemoIdAndStatus(memoId, ReviewStatus.ACTIVE).isPresent()) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
        }

        List<ValidFindingDraft> drafts = analyzeAndSanitize(memoId, memo.getTitle(), content, List.of(), List.of());

        try {
            return reviewWriter.saveNewReview(memoId, content, drafts);
        } catch (DataIntegrityViolationException e) {
            throw toInProgressIfVersionConflict(e);
        }
    }

    // 재료 준비(읽기) → AI 분석(트랜잭션 밖, 반영 안 한 카드 문장은 제외하라고 알림) → 새 버전 저장(짧은 트랜잭션).
    // AI가 실패하면 저장 단계까지 가지 않으므로 지금 버전이 그대로 남는다
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReviewResponse rerunReview(Long userId, Long reviewId) {
        RerunInput input = reviewWriter.prepareRerun(userId, reviewId);
        validateContentLength(input.content());

        List<ValidFindingDraft> drafts = analyzeAndSanitize(
                input.memoId(), input.title(), input.content(), input.excludedAnchors(), input.resolvedGaps());

        try {
            return reviewWriter.saveRerun(input.memoId(), reviewId, input.content(), drafts, reviewProperties.getMaxFindings());
        } catch (DataIntegrityViolationException e) {
            throw toInProgressIfVersionConflict(e);
        }
    }

    // 저장 전 메모 잠금으로 막지만, 혹시 같은 버전을 두 번 저장하려 하면 (memo_id, version) 유니크 제약이 마지막으로 막는다.
    // 그 경우만 '진행 중' 409로 바꾸고, 다른 DB 오류(글자 수 초과 등)는 원인이 로그에 남도록 그대로 던진다
    private RuntimeException toInProgressIfVersionConflict(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains(VERSION_UNIQUE_CONSTRAINT)) {
                return new ReviewException(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
            }
        }
        return e;
    }

    private List<ValidFindingDraft> analyzeAndSanitize(Long memoId, String title, String content,
                                                       List<String> excludedAnchors, List<ResolvedGap> resolvedGaps) {
        AnalysisResult analysis = reviewAiClient.analyze(memoId, title, content, excludedAnchors, resolvedGaps);
        List<ValidFindingDraft> drafts = FindingDrafts.sanitize(
                content, analysis.findings(), reviewProperties.getMaxFindings());

        // AI가 빈틈을 줬는데 전부 본문에서 못 찾았다면 '빈틈 0개'가 아니라 분석 실패다
        if (drafts.isEmpty() && !analysis.findings().isEmpty()) {
            throw new ReviewException(ReviewErrorCode.REVIEW_AI_FAILED);
        }
        return drafts;
    }

    // 반영 문장은 반영할 때 이미 본문에 들어갔고 재임베딩도 끝났으므로, 상태만 '종료'로 바꾼다.
    // 반영하지 않은 카드는 화면에서만 사라지고 기록(버전)에는 남는다
    @Override
    @Transactional
    public EndReviewResponse endReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .filter(Review::isActive)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));
        Memo memo = review.getMemo();
        if (Boolean.TRUE.equals(memo.getIsDeleted())) {
            throw new MemoException(MemoErrorCode.MEMO_NOT_FOUND);
        }
        if (!memo.getUser().getId().equals(userId)) {
            throw new MemoException(MemoErrorCode.FORBIDDEN_MEMO);
        }

        review.end();
        return new EndReviewResponse(memo.getId(), review.getVersion());
    }

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

    // 본문만(제목 제외), 공백 포함으로 센다. 이모지 등이 2칸으로 세지지 않도록 코드 포인트 기준
    private void validateContentLength(String content) {
        int length = content == null ? 0 : content.codePointCount(0, content.length());
        if (length < reviewProperties.getMinContentLength()) {
            throw ReviewLengthException.contentTooShort(reviewProperties.getMinContentLength());
        }
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
