package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiMemoCreateResponse(
        @Schema(description = "생성된 메모 ID", example = "13")
        Long memoId,
        @Schema(description = "생성된 메모 제목", example = "RAG 정리 메모 2026-01-17 05:10")
        String title,
        @Schema(description = "생성된 메모 본문", example = "핵심 요약 내용...")
        String content,
        @Schema(description = "RAG 검색으로 사용된 컨텍스트 청크 개수", example = "6")
        int contextCount,
        @Schema(description = "실제로 적용된 시스템 프롬프트", example = "너는 나만의 똑똑한 메모 정리 AI야..~")
        String appliedSystemPrompt,
        @Schema(description = "실제로 적용된 temperature", example = "0.7")
        Double appliedTemperature
) {
    public static AiMemoCreateResponse of(
            Long memoId,
            String title,
            String content,
            int contextCount,
            String appliedSystemPrompt,
            Double appliedTemperature
    ) {
        return new AiMemoCreateResponse(
                memoId,
                title,
                content,
                contextCount,
                appliedSystemPrompt,
                appliedTemperature
        );
    }
}
