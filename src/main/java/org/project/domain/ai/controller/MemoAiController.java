package org.project.domain.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.rag.pipeline.RagPipeline;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
