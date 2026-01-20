package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.request.MemoAiRequestForPlan;
import org.project.domain.ai.dto.response.AiEvaluationResult;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.dto.response.MemoAiResponseForPlan;
import org.project.domain.ai.rag.pipeline.RagPipeline;
import org.project.domain.ai.service.AiEvaluationService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/memo")
@RequiredArgsConstructor
@Tag(
        name = "AI Memo",
        description = "메모 기반 RAG AI 생성 API"
)
public class MemoAiController {

    private final RagPipeline ragPipeline;
    private final AiEvaluationService aiEvaluationService;


    @Operation(
            summary = "AI 메모 응답 생성",
            description = """
                선택한 메모들을 기반으로 RAG를 수행하여
                AI 응답을 생성합니다.

                - chatRoomId : AI 대화 세션 ID
                - userPrompt : 사용자의 질문
                - option     : AI 처리 전략 (MERGE, STRUCTURE, SUMMARY)
                - memoIds    : 참조할 메모 ID 목록

                채팅방(chatRoom) 단위로 대화 컨텍스트가 유지됩니다.
                """
    )
    @PostMapping("/chat-rooms/{chatRoomId}/memo")
    public ResponseEntity<ApiResponse<MemoAiResponse>> generateMemoAi(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long chatRoomId,
            @Valid @RequestBody MemoAiRequest request
    ) {

        MemoAiResponse response = ragPipeline.run(
                userDetails.getUserId(),
                chatRoomId,
                request
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }


    @Operation(
            summary = "[기획 의사결정용] AI 응답 생성 + 품질 지표 평가",
            description = """
                기획에서 정의한 System Prompt를 기반으로 AI 응답을 생성하고,
                생성된 응답에 대해 품질 지표를 함께 반환합니다.

                ---
                기본 동작
                - 이 API에서는 option 값은 실제 처리 로직에는 사용되지 않습니다.
                - AI의 동작 방식은 전적으로 systemPrompt에 의해 결정됩니다.
                - memoIds는 RAG 컨텍스트로 사용되며,
                  systemPrompt는 최상위 SYSTEM 메시지로 적용됩니다.

                ---
                응답 품질 평가 지표 설명

                1. Relevance Score (0.0 ~ 1.0)
                - 응답이 사용자 요청 의도와 얼마나 잘 부합하는지 나타내는 지표
                - 0.8↑ 적합, 0.5~0.8 부분적 적합, 0.5↓ 부적합

                2. Prompt Faithfulness Score (0.0 ~ 1.0)
                - 응답이 System Prompt를 얼마나 충실히 따랐는지 나타내는 지표
                - 1.0 완전 준수, 0.7~0.9 경미한 이탈, 0.7↓ 위반 가능

                3. Groundedness Score (0.0 ~ 1.0)
                - 응답이 제공된 문서 기반으로 생성되었는지를 나타내는 지표
                - 0.8↑ 문서 기반, 0.5~0.8 일부 추론, 0.5↓ 근거 부족

                4. Task Alignment Pass (true / false)
                - 사용자 요청이 요구한 작업을 제대로 수행했는지를 나타내는 지표
                - true  : 요청 의도에 맞는 작업 수행
                - false : 작업 미이행 또는 의도 불일치
                """
    )
    @PostMapping("/for-plan")
    public ResponseEntity<ApiResponse<MemoAiResponseForPlan>> generateMemoAiForPlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoAiRequestForPlan request
    ) {
        // AI 요청 DTO 구성
        MemoAiRequest memoRequest =
                MemoAiRequest.of(
                        request.userPrompt(),
                        request.option(),
                        request.memoIds()
                );

        // AI 응답 생성
        MemoAiResponse aiResponse =
                ragPipeline.runForPlan(
                        userDetails.getUserId(),
                        memoRequest,
                        request.systemPrompt()
                );

        // AI 응답 품질 평가
        AiEvaluationResult evaluationResult =
                aiEvaluationService.evaluate(
                        request.userPrompt(),
                        aiResponse
                );

        // 응답 결합
        MemoAiResponseForPlan response =
                MemoAiResponseForPlan.of(
                        aiResponse,
                        evaluationResult
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
