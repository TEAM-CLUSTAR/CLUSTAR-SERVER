package org.project.domain.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.review.ai.ReviewAiClient;
import org.project.domain.review.ai.dto.RefineInput;
import org.project.domain.review.dto.request.ApplyType;
import org.project.domain.review.dto.response.ApplyResponse;
import org.project.domain.review.dto.response.RefineResponse;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.dto.response.AnswerResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.domainException.ReviewLengthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewFindingServiceImpl implements ReviewFindingService {

    private final ReviewWriter reviewWriter;
    private final ReviewAiClient reviewAiClient;
    private final ReviewProperties reviewProperties;

    // 답 저장(짧은 트랜잭션) → 첫 답이면 AI 후속 질문(트랜잭션 밖) → 질문 저장.
    // AI가 실패해도 사용자 답은 이미 저장돼 있으므로 에러 대신 aiMessage 없이 응답한다
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AnswerResponse answer(Long userId, Long reviewId, Long findingId, String content) {
        validateAnswerLength(content);

        SavedAnswer saved = reviewWriter.saveAnswer(userId, reviewId, findingId, content);
        ReviewMessageResponse aiMessage = saved.followUpInput() == null
                ? null
                : createFollowUp(findingId, saved);

        return new AnswerResponse(findingId, saved.status(), saved.answer(), aiMessage);
    }

    // 재료 준비(읽기 트랜잭션) → AI 다듬기(트랜잭션 밖). 미리보기는 저장하지 않는다(취소는 화면만 닫으면 끝)
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefineResponse refine(Long userId, Long reviewId, Long findingId) {
        RefineInput input = reviewWriter.prepareRefine(userId, reviewId, findingId);
        String refinedText = reviewAiClient.refine(input);
        return new RefineResponse(findingId, refinedText, input.rawText());
    }

    // REFINED는 미리보기에서 고친 문장을 받고(모두 지웠으면 400), RAW는 서버가 저장된 답으로 만든다
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApplyResponse apply(Long userId, Long reviewId, Long findingId, ApplyType type, String text) {
        String appliedText = null;
        if (type == ApplyType.REFINED) {
            if (text == null || text.isBlank()) {
                throw new ReviewException(ReviewErrorCode.APPLY_TEXT_REQUIRED);
            }
            appliedText = text.strip();
        }
        return reviewWriter.apply(userId, reviewId, findingId, type, appliedText);
    }

    private ReviewMessageResponse createFollowUp(Long findingId, SavedAnswer saved) {
        try {
            String question = reviewAiClient.followUp(saved.followUpInput());
            return reviewWriter.saveAiMessage(findingId, question);
        } catch (RuntimeException e) {
            // 답은 이미 저장됐다. 질문만 없을 뿐이니 실패로 응답하지 않는다(실패로 보이면 같은 답을 다시 보내 중복된다)
            log.warn("후속 질문 없이 응답 [findingId={}, error={}]", findingId, e.getMessage(), e);
            return null;
        }
    }

    private void validateAnswerLength(String content) {
        int length = content.codePointCount(0, content.length());
        if (length > reviewProperties.getMaxAnswerLength()) {
            throw ReviewLengthException.answerTooLong(reviewProperties.getMaxAnswerLength());
        }
    }
}
