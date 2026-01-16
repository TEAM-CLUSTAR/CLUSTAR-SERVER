package org.project.domain.ai.dto.response;

import org.project.domain.ai.entity.ContextType;

/**
 * RAG 검색 결과를 서비스 계층에서 전달하기 위한 DTO.
 */
public record RagContextChunkResponse(
        ContextType contextType,
        Long contextId,
        Integer chunkIndex,
        String chunkText
) {
}
