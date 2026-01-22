package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.request.MemoAiRequestForPlan;
import org.project.domain.ai.dto.response.AiEvaluationResult;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.dto.response.MemoAiResponseForPlan;
import org.project.domain.ai.rag.pipeline.RagPipeline;
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

        // 파이프라인 실행
        return ragPipeline.run(
                userId,
                chatRoomId,
                request
        );
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

        // 응답 조립
        return MemoAiResponseForPlan.of(
                aiResponse,
                evaluationResult
        );
    }
}
