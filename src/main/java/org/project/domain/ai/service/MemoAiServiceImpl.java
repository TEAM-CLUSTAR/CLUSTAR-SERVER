package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.request.MemoAiRequestForPlan;
import org.project.domain.ai.dto.response.AiEvaluationResult;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.dto.response.MemoAiResponseForPlan;
import org.project.domain.ai.rag.pipeline.RagPipeline;
import org.project.global.exception.InsufficientRagContextException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemoAiServiceImpl implements MemoAiService {

    private final RagPipeline ragPipeline;
    private final ChatRoomService chatRoomService;

    private final AiEvaluationService aiEvaluationService;

    @Override
    public MemoAiResponse generate(
            Long userId,
            Long chatRoomId,
            MemoAiRequest request
    ) {

        // 접근 검증
        chatRoomService.validateAccess(userId, chatRoomId);

        try {
            // 정상 RAG 파이프라인 실행
            return ragPipeline.run(
                    userId,
                    chatRoomId,
                    request
            );

        } catch (InsufficientRagContextException e) {

            return MemoAiResponse.of(
                    /* title */ "AI 응답을 생성할 수 없습니다",
                    /* content */ e.getMessage(),
                    /* option */ request.option(),
                    /* memoIds */ request.memoIds(),
                    /* debug */ null   // or "CONTEXT_TOO_SHORT"
            );
        }
    }

    @Override
    public MemoAiResponseForPlan generateForPlan(
            Long userId,
            Long chatRoomId,
            MemoAiRequestForPlan request
    ) {

        // 채팅방 접근 검증
        chatRoomService.validateAccess(userId, chatRoomId);

        // 내부 공통 AI 요청 DTO로 변환
        MemoAiRequest memoRequest = MemoAiRequest.of(
                request.userPrompt(),
                request.option(),
                request.memoIds()
        );

        try {
            // AI 응답 생성
            MemoAiResponse aiResponse = ragPipeline.runForPlan(
                    userId,
                    chatRoomId,
                    memoRequest,
                    request.systemPrompt(),
                    request.model(),
                    request.temperature()
            );

            // AI 응답 품질 평가
            AiEvaluationResult evaluationResult =
                    aiEvaluationService.evaluate(
                            request.userPrompt(),
                            aiResponse
                    );

            // 정상 응답
            return MemoAiResponseForPlan.of(
                    aiResponse,
                    evaluationResult
            );

        } catch (InsufficientRagContextException e) {
            // 컨텍스트 부족 → AI 응답처럼 치환

            MemoAiResponse fallbackResponse = MemoAiResponse.of(
                    "AI 응답을 생성할 수 없습니다",
                    e.getMessage(),
                    request.option(),
                    request.memoIds(),
                    null
            );

            // 컨텍스트 부족은 "평가 대상 아님"
            AiEvaluationResult emptyEvaluation =
                    AiEvaluationResult.of(
                            0.0,
                            0.0,
                            0.0,
                            false
                    );

            return MemoAiResponseForPlan.of(
                    fallbackResponse,
                    emptyEvaluation
            );
        }
    }
}
