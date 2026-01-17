package org.project.domain.ai.service;

import org.project.domain.ai.dto.response.RagContextChunkResponse;

import java.util.List;

public interface ContextEmbeddingSearchService {

    List<RagContextChunkResponse> searchTopK(
            Long userId,
            String queryText,
            List<Long> memoIds,
            int topK
    );
}
