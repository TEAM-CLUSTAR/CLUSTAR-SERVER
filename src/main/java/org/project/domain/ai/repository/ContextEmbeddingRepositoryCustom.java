package org.project.domain.ai.repository;

import org.project.domain.ai.dto.ContextEmbeddingWithScore;

import java.util.List;

public interface ContextEmbeddingRepositoryCustom {

    List<ContextEmbeddingWithScore> searchByMemoIds(
            List<Long> memoIds,
            float[] queryEmbedding,
            int limit
    );
}
