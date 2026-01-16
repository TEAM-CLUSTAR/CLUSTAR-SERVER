package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.service.MemoAiService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.annotation.BusinessExceptionDescription;
import org.project.global.config.swagger.SwaggerResponseDescription;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final MemoAiService aiService;

    @Operation(summary = "메모 AI 채팅",
            description = "메모ids, 프롬프트, 옵션으로 AI채팅을 요청합니다.\n" +
                    "옵션 : MERGE, STRUCTURE, SUMMARY")
    @PostMapping("/memos")
    @BusinessExceptionDescription(SwaggerResponseDescription.MEMO_AI)
    public ResponseEntity<ApiResponse<MemoAiResponse>> generateMemoAi(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MemoAiRequest request
    ) {
        MemoAiResponse response = aiService.generateMemoAi(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
