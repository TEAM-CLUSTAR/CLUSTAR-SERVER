package org.project.domain.ai.rag.E.retrieve.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoSearchVectorRetriever {

    private final VectorStore vectorStore;
    private final MemoRepository memoRepository;

    private static final int VECTOR_TOP_K = 10;
    private static final int MAX_MEMO_RESULTS = 2;

    public List<Memo> retrieve(Long userId, String query) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();

            var searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(VECTOR_TOP_K)
                    .filterExpression(b.eq("userId", userId).build())
                    .build();

            // memoId 기준 중복 제거 (LinkedHashMap으로 유사도 순서 유지)
            Map<Long, Long> seenMemoIds = new LinkedHashMap<>();

            vectorStore.similaritySearch(searchRequest).forEach(doc -> {
                Object memoIdObj = doc.getMetadata().get("memoId");
                if (memoIdObj instanceof Number memoId) {
                    seenMemoIds.putIfAbsent(memoId.longValue(), memoId.longValue());
                }
            });

            List<Long> topMemoIds = new ArrayList<>(seenMemoIds.keySet())
                    .subList(0, Math.min(MAX_MEMO_RESULTS, seenMemoIds.size()));

            if (topMemoIds.isEmpty()) {
                return List.of();
            }

            return memoRepository.findByIdInWithLabelsAndNotDeleted(userId, topMemoIds);

        } catch (Exception e) {
            log.warn("[MemoSearchVectorRetriever] 벡터 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
