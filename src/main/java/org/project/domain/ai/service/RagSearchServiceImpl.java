package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.ContextEmbeddingWithScore;
import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.entity.ContextType;
import org.project.domain.ai.repository.ContextEmbeddingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagSearchServiceImpl implements RagSearchService {

    private static final double SIMILARITY_THRESHOLD = 0.75;

    private final ContextEmbeddingRepository embeddingRepository;

    @Override
    public List<ContextEmbedding> searchRelevantChunks(
            List<Long> memoIds,
            float[] queryEmbedding,
            int topK
    ) {

        List<ContextEmbeddingWithScore> results =
                embeddingRepository.searchByMemoIds(
                        memoIds,
                        queryEmbedding,
                        topK * 2
                );

        return results.stream()
                .filter(r -> r.getSimilarity() >= SIMILARITY_THRESHOLD)
                .limit(topK)
                .map(r -> ContextEmbedding.builder()
                        .id(r.getId())
                        .contextType(ContextType.valueOf(r.getContextType()))
                        .contextId(r.getContextId())
                        .memoId(r.getMemoId())
                        .chunkIndex(r.getChunkIndex())
                        .chunkedContent(r.getChunkedContent())
                        .model(r.getModel())
                        .build()
                )
                .toList();
    }

}

