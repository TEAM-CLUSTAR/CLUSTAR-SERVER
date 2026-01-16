package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.RagPromptConfigRequest;
import org.project.domain.ai.dto.response.RagPromptConfigResponse;
import org.project.domain.ai.service.PromptConfigService;
import org.project.domain.ai.strategy.MemoAiOptions;
import org.project.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/prompt-config")
public class PromptConfigController {

    private final PromptConfigService promptConfigService;

    @Operation(summary = "RAG 프롬프트 설정 조회", description = "옵션별 시스템 프롬프트/온도 설정을 조회합니다.")
    @GetMapping("/{option}")
    public ResponseEntity<ApiResponse<RagPromptConfigResponse>> getConfig(
            @PathVariable MemoAiOptions option
    ) {
        RagPromptConfigResponse response = promptConfigService.get(option);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "RAG 프롬프트 설정 수정", description = "옵션별 시스템 프롬프트/온도 설정을 수정합니다.")
    @PutMapping("/{option}")
    public ResponseEntity<ApiResponse<RagPromptConfigResponse>> updateConfig(
            @PathVariable MemoAiOptions option,
            @RequestBody @Valid RagPromptConfigRequest request
    ) {
        RagPromptConfigResponse response = promptConfigService.upsert(option, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
