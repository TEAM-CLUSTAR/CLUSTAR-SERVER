package org.project.domain.review.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.event.MemoTextUpdatedEvent;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.ai.dto.FollowUpInput;
import org.project.domain.review.ai.dto.QuestionAnswers;
import org.project.domain.review.ai.dto.RefineInput;
import org.project.domain.review.dto.request.ApplyType;
import org.project.domain.review.dto.response.ApplyResponse;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewMessage;
import org.project.domain.review.entity.ReviewMessageRole;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewMessageRepository;
import org.project.domain.review.repository.ReviewRepository;
import org.project.domain.review.support.AppliedTextInserter;
import org.project.domain.review.support.ValidFindingDraft;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final ReviewMessageRepository messageRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    // 사용자 답을 저장하고 카드를 '답하는 중'으로 바꾼다.
    // 첫 답이면 후속 질문에 쓸 재료를 함께 돌려준다(AI 호출은 트랜잭션 밖에서 한다)
    public SavedAnswer saveAnswer(Long userId, Long reviewId, Long findingId, String content) {
        ReviewFinding finding = getAnswerableFinding(userId, reviewId, findingId);
        boolean firstAnswer = messageRepository.countByFindingIdAndRole(findingId, ReviewMessageRole.USER) == 0;

        ReviewMessage answer = messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, content));
        finding.markAnswering();

        FollowUpInput followUpInput = firstAnswer ? toFollowUpInput(finding, content) : null;
        return new SavedAnswer(ReviewMessageResponse.from(answer), finding.getStatus(), followUpInput);
    }

    // 다듬기 재료를 모은다(쓰기 없음). 대화를 순서대로 보며 AI 후속 질문이 나오면 새 질문으로 넘기고,
    // 사용자 답은 직전 질문에 붙인다. 답이 없는 질문은 뺀다
    @Transactional(readOnly = true)
    public RefineInput prepareRefine(Long userId, Long reviewId, Long findingId) {
        ReviewFinding finding = getAnswerableFinding(userId, reviewId, findingId);
        List<ReviewMessage> messages = messageRepository.findByFindingIdInOrderByIdAsc(List.of(findingId));

        List<QuestionAnswers> questionAnswers = new ArrayList<>();
        String currentQuestion = finding.getQuestion();
        List<String> currentAnswers = new ArrayList<>();
        for (ReviewMessage message : messages) {
            if (message.getRole() == ReviewMessageRole.AI) {
                addIfAnswered(questionAnswers, currentQuestion, currentAnswers);
                currentQuestion = message.getContent();
                currentAnswers = new ArrayList<>();
            } else {
                currentAnswers.add(message.getContent());
            }
        }
        addIfAnswered(questionAnswers, currentQuestion, currentAnswers);

        if (questionAnswers.isEmpty()) {
            throw new ReviewException(ReviewErrorCode.ANSWER_REQUIRED);
        }
        Memo memo = finding.getReview().getMemo();
        return new RefineInput(memo.getId(), finding.getSectionLabel(), finding.getAnchorText(), questionAnswers);
    }

    private void addIfAnswered(List<QuestionAnswers> questionAnswers, String question, List<String> answers) {
        if (!answers.isEmpty()) {
            questionAnswers.add(new QuestionAnswers(question, List.copyOf(answers)));
        }
    }

    public ReviewMessageResponse saveAiMessage(Long findingId, String content) {
        ReviewFinding finding = findingRepository.getReferenceById(findingId);
        return ReviewMessageResponse.from(messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.AI, content)));
    }

    // 짚은 문장 다음 줄에 반영 문장을 넣고 카드를 '반영됨'으로 바꾼다. 재임베딩 이벤트도 낸다.
    // 같은 카드를 두 번 반영하지 않도록 카드 행을, 같은 메모에 동시에 반영해도 본문이 덮어써지지 않도록 메모 행을 잠근다
    public ApplyResponse apply(Long userId, Long reviewId, Long findingId, ApplyType type, String text) {
        // 잠근 뒤에 읽어야 다른 요청이 바꾼 최신 상태를 본다: 카드 잠금 → 카드 읽기 → 메모 잠금 → 메모 읽기
        findingRepository.lockById(findingId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.FINDING_NOT_FOUND));
        ReviewFinding finding = findInReview(findingRepository.findById(findingId), reviewId);
        lockMemo(finding.getReview().getMemo().getId());
        Memo memo = finding.getReview().getMemo();
        validateAnswerable(finding, memo, userId);

        String appliedText = type == ApplyType.RAW ? rawAnswers(findingId) : text;
        String newContent = AppliedTextInserter.insertAfterAnchorLine(memo.getContent(), finding.getAnchorText(), appliedText)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.ANCHOR_NOT_FOUND));

        memo.update(memo.getTitle(), newContent);
        finding.apply(appliedText);
        // updatedAt을 응답에 담기 위해 flush(기존 메모 수정 API와 같은 방식)
        memoRepository.flush();
        // 검색·추천이 새 본문을 보도록 기존 재임베딩 리스너에 맡긴다(커밋 후 비동기)
        eventPublisher.publishEvent(new MemoTextUpdatedEvent(memo.getId(), userId));

        return ApplyResponse.of(memo, finding);
    }

    // 메모 행 잠금. MemoRepository는 건드리지 않고 같은 방식(네이티브 FOR UPDATE)으로 잠근다
    private void lockMemo(Long memoId) {
        entityManager.createNativeQuery("SELECT memo_id FROM memo WHERE memo_id = :memoId FOR UPDATE")
                .setParameter("memoId", memoId)
                .getResultList();
    }

    // 내 답 원문: 보낸 순서대로 줄바꿈으로 잇는다(다듬기 응답의 rawText와 같은 규칙)
    private String rawAnswers(Long findingId) {
        List<String> answers = messageRepository.findByFindingIdInOrderByIdAsc(List.of(findingId)).stream()
                .filter(message -> message.getRole() == ReviewMessageRole.USER)
                .map(ReviewMessage::getContent)
                .toList();
        if (answers.isEmpty()) {
            throw new ReviewException(ReviewErrorCode.ANSWER_REQUIRED);
        }
        return String.join("\n", answers);
    }

    // 카드가 이 검토의 것인지 → 검토가 진행 중인지 → 메모가 살아 있고 내 것인지 → 아직 반영 전인지
    private ReviewFinding getAnswerableFinding(Long userId, Long reviewId, Long findingId) {
        ReviewFinding finding = findInReview(findingRepository.findById(findingId), reviewId);
        validateAnswerable(finding, finding.getReview().getMemo(), userId);
        return finding;
    }

    private ReviewFinding findInReview(Optional<ReviewFinding> found, Long reviewId) {
        return found
                .filter(finding -> finding.getReview().getId().equals(reviewId))
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.FINDING_NOT_FOUND));
    }

    private void validateAnswerable(ReviewFinding finding, Memo memo, Long userId) {
        if (!finding.getReview().isActive()) {
            throw new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(memo.getIsDeleted())) {
            throw new MemoException(MemoErrorCode.MEMO_NOT_FOUND);
        }
        if (!memo.getUser().getId().equals(userId)) {
            throw new MemoException(MemoErrorCode.FORBIDDEN_MEMO);
        }
        if (finding.isApplied()) {
            throw new ReviewException(ReviewErrorCode.FINDING_ALREADY_APPLIED);
        }
    }

    private FollowUpInput toFollowUpInput(ReviewFinding finding, String answer) {
        Memo memo = finding.getReview().getMemo();
        return new FollowUpInput(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                finding.getSectionLabel(),
                finding.getAnchorText(),
                finding.getReason(),
                finding.getEvidence(),
                finding.getQuestion(),
                answer
        );
    }

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
