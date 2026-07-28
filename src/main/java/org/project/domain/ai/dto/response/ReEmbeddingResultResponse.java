package org.project.domain.ai.dto.response;

public record ReEmbeddingResultResponse(
        Long memoId,
        Boolean textSucceeded,   // 항상 시도됨
        Boolean imageSucceeded,  // null = 이미지 없음(시도 안 함)
        Boolean fileSucceeded    // null = 파일 없음(시도 안 함)
) {
}
