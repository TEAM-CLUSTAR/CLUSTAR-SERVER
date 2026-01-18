package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.request.MemoAiRequestForPlan;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.rag.pipeline.RagPipeline;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/memo")
@RequiredArgsConstructor
@Tag(
        name = "AI Memo",
        description = "메모 기반 RAG AI 생성 API"
)
public class MemoAiController {

    private final RagPipeline ragPipeline;

    @Operation(
            summary = "메모 기반 AI 응답 생성",
            description = """
                    선택한 메모들을 기반으로 RAG를 수행하여
                    AI 응답을 생성합니다.
                    
                    - userPrompt: 사용자의 질문
                    - option: AI 처리 전략 (MERGE, STRUCTURE, SUMMARY)
                    - memoIds: 참조할 메모 ID 목록
                    
                    기본 AI 채팅/요약/정리 용도로 사용됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<MemoAiResponse>> generateMemoAi(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoAiRequest request
    ) {

        MemoAiResponse response = ragPipeline.run(
                userDetails.getUserId(),
                request
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "플랜 기반 AI 응답 생성",
            description = """
                    기획에서 정의한 System Prompt를 기반으로 AI 응답을 생성합니다.
                    
                    - 이 API에서는 option 값은 실제 처리에 사용되지 않습니다.
                    - AI의 동작 방식은 전적으로 systemPrompt에 의해 결정됩니다.
                    
                    memoIds는 RAG 컨텍스트로 사용되며,
                    systemPrompt는 최상위 SYSTEM 메시지로 적용됩니다.
                    """
    )
    @PostMapping("/for-plan")
    public ResponseEntity<ApiResponse<MemoAiResponse>> generateMemoAiForPlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemoAiRequestForPlan request
    ) {
        MemoAiRequest memoRequest =
                MemoAiRequest.of(
                        request.userPrompt(),
                        request.option(),
                        request.memoIds()
                );

        MemoAiResponse response =
                ragPipeline.runForPlan(
                        userDetails.getUserId(),
                        memoRequest,
                        request.systemPrompt()
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
