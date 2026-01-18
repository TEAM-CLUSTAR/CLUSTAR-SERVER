package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.repository.ContextEmbeddingSearchRepository;
import org.project.domain.ai.dto.response.RagContextChunkResponse;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class ContextEmbeddingSearchServiceImpl implements ContextEmbeddingSearchService {

    private final EmbeddingModel embeddingModel;
    private final ContextEmbeddingSearchRepository embeddingSearchRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

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

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            SearchRequest request = SearchRequest.builder()
                    .query(queryText)
                    .topK(topK)
                    .filterExpression(buildFilterExpression(userId, memoIds))
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);
            return results.stream()
                    .map(ContextEmbeddingSearchServiceImpl::mapDocument)
                    .toList();
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

    private Filter.Expression buildFilterExpression(Long userId, List<Long> memoIds) {
        Filter.Expression base = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("userId"),
                new Filter.Value(userId.toString())
        );

        if (memoIds == null || memoIds.isEmpty()) {
            return base;
        }

        List<String> memoIdValues = memoIds.stream()
                .map(String::valueOf)
                .toList();

        Filter.Expression memoFilter = new Filter.Expression(
                Filter.ExpressionType.IN,
                new Filter.Key("memoId"),
                new Filter.Value(memoIdValues)
        );

        return new Filter.Expression(
                Filter.ExpressionType.AND,
                base,
                memoFilter
        );
    }

    private static RagContextChunkResponse mapDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object contextType = metadata.get("contextType");
        Object contextId = metadata.get("contextId");
        Object chunkIndex = metadata.get("chunkIndex");

        if (contextType == null || contextId == null || chunkIndex == null) {
            throw new AiException(AiErrorCode.RAG_CONTEXT_NOT_FOUND);
        }

        return new RagContextChunkResponse(
                org.project.domain.ai.entity.ContextType.valueOf(contextType.toString()),
                Long.parseLong(contextId.toString()),
                Integer.parseInt(chunkIndex.toString()),
                document.getText()
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
