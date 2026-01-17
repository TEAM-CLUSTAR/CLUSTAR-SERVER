package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.repository.ContextEmbeddingSearchRepository;
import org.project.domain.ai.dto.response.RagContextChunkResponse;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class ContextEmbeddingSearchServiceImpl implements ContextEmbeddingSearchService {

    private final EmbeddingModel embeddingModel;
    private final ContextEmbeddingSearchRepository embeddingSearchRepository;

    @Override
    public List<RagContextChunkResponse> searchTopK(
            Long userId,
            String queryText,
            List<Long> memoIds,
            int topK
    ) {
        if (queryText == null || queryText.isBlank()) {
            throw new AiException(AiErrorCode.EMPTY_EMBEDDING_TEXT);
        }

        float[] queryVector = embeddingModel.embed(queryText);
        String vectorLiteral = toVectorLiteral(queryVector);

        return embeddingSearchRepository.searchTopK(
                userId,
                memoIds,
                vectorLiteral,
                topK
        );
    }

    /**
     * pgvector에서 인식 가능한 literal 문자열로 변환한다. 예: [0.1,0.2,0.3]
     */
    private String toVectorLiteral(float[] vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            joiner.add(Float.toString(v));
        }
        return joiner.toString();
    }
}
