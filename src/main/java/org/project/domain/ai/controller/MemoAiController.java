package org.project.domain.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/memo")
@RequiredArgsConstructor
public class MemoAiController {

    private final RagPipeline ragPipeline;

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

    @PostMapping("/for-plan")
    public ResponseEntity<ApiResponse<MemoAiResponse>> generateMemoAiForPlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Object> body
    ) {
        ObjectMapper objectMapper = new ObjectMapper();

        // MemoAiRequest 역직렬화
        MemoAiRequest request =
                objectMapper.convertValue(body, MemoAiRequest.class);

        // 추가 String 값
        String planPrompt = (String) body.get("planPrompt");

        MemoAiResponse response = ragPipeline.runForPlan(
                userDetails.getUserId(),
                request,
                planPrompt
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
