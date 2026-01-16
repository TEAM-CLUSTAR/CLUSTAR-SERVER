package org.project.domain.ai.dto;

import org.project.domain.ai.entity.ContextEmbedding;

public record ContextEmbeddingWithScore(
        ContextEmbedding embedding,
        double similarity
) {
}
