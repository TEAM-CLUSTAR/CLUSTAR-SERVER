package org.project.domain.ai.dto.response;

public record RagMemoCreateResponse(
        Long memoId,
        String title,
        String content,
        int contextCount
) {
    public static RagMemoCreateResponse of(Long memoId, String title, String content, int contextCount) {
        return new RagMemoCreateResponse(memoId, title, content, contextCount);
    }
}
