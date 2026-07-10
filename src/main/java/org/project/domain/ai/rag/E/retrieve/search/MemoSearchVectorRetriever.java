package org.project.domain.ai.rag.E.retrieve.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.config.MemoSearchProperties;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.springframework.ai.document.DocumentMetadata;
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
    private final MemoSearchProperties memoSearchProperties;

    private static final int VECTOR_TOP_K = 10;
    private static final int MAX_MEMO_RESULTS = 2;

    public List<Memo> retrieve(Long userId, String query) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();

            // threshold는 여기서 걸지 않고 topK 후보를 전부 받아서 애플리케이션 레벨에서 거른다.
            // (SearchRequest에 similarityThreshold를 걸면 컷된 후보는 아예 안 돌아와서, 로그로도 못 남기고
            //  나중에 threshold를 재조정할 때 "얼마나 아깝게 컷됐는지"를 알 수 없게 된다.)
            var searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(VECTOR_TOP_K)
                    .similarityThresholdAll()
                    .filterExpression(b.eq("userId", userId).build())
                    .build();

            double threshold = memoSearchProperties.getSemanticSimilarityThreshold();

            // memoId 기준 중복 제거 (LinkedHashMap으로 유사도 순서 유지)
            Map<Long, Long> seenMemoIds = new LinkedHashMap<>();

            vectorStore.similaritySearch(searchRequest).forEach(doc -> {
                Object memoIdObj = doc.getMetadata().get("memoId");
                Object distanceObj = doc.getMetadata().get(DocumentMetadata.DISTANCE.value());
                double similarity = distanceObj instanceof Number distance ? 1 - distance.doubleValue() : 0;
                boolean passed = similarity >= threshold;

                // 향후 threshold 재조정을 위해 통과/탈락 여부와 무관하게 전부 로그로 남긴다 (임시값, 실사용 데이터로 재산정 예정)
                log.info("[Search][Semantic] userId={} query=\"{}\" memoId={} similarity={} threshold={} passed={}",
                        userId, query, memoIdObj, similarity, threshold, passed);

                if (passed && memoIdObj instanceof Number memoId) {
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
