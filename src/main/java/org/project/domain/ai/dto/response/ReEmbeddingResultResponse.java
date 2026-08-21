package org.project.domain.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"memoId", "textSucceeded", "imageSucceeded", "fileSucceeded"})
public record ReEmbeddingResultResponse(
        Long memoId,
        Boolean textSucceeded,   // 항상 시도됨
        @Schema(nullable = true, description = "이미지 재임베딩 성공 여부. 이미지 첨부가 없어 시도하지 않은 경우 null")
        Boolean imageSucceeded,
        @Schema(nullable = true, description = "파일 재임베딩 성공 여부. 파일 첨부가 없어 시도하지 않은 경우 null")
        Boolean fileSucceeded
) {
}
