package org.project.domain.ai.service;

import org.project.domain.ai.entity.ContextEmbedding;

import java.util.List;

public interface RagSearchService {

    List<ContextEmbedding> searchRelevantChunks(
            List<Long> memoIds,
            float[] queryEmbedding,
            int topK
    );
}
