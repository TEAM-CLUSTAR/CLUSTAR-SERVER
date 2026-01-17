package org.project.domain.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.AiPromptConfigRequest;
import org.project.domain.ai.dto.response.AiPromptResponse;
import org.project.domain.ai.service.AiPromptConfigService;
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
@RequestMapping("/api/v1/ai/prompt")
@Tag(name = "AI 메모 프롬프팅 관련", description = "AI 프롬프팅을 할 수 있는 API")
public class AiPromptController {

    private final AiPromptConfigService aiPromptConfigService;

    @Operation(summary = "RAG 프롬프트 설정 조회", description = "옵션별 시스템 프롬프트/온도 설정을 조회합니다.")
    @GetMapping("/{option}")
    public ResponseEntity<ApiResponse<AiPromptResponse>> getConfig(
            @PathVariable MemoAiOptions option
    ) {
        AiPromptResponse response = aiPromptConfigService.get(option);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "RAG 프롬프트 설정 수정", description = "옵션별 시스템 프롬프트/온도 설정을 수정합니다.")
    @PutMapping("/{option}")
    public ResponseEntity<ApiResponse<AiPromptResponse>> updateConfig(
            @PathVariable MemoAiOptions option,
            @RequestBody @Valid AiPromptConfigRequest request
    ) {
        AiPromptResponse response = aiPromptConfigService.upsert(option, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
