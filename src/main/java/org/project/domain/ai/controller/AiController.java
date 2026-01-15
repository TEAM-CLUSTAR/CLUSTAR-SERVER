package org.project.domain.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.service.MemoAiService;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final MemoAiService aiService;

    @PostMapping("/memos")
    public ResponseEntity<ApiResponse<MemoAiResponse>> generateMemoAi(
            @RequestBody @Valid MemoAiRequest request
    ) {
        MemoAiResponse response = aiService.generateMemoAi(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
