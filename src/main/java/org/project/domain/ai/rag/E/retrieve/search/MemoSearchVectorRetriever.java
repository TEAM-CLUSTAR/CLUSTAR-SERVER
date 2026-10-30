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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoSearchVectorRetriever {

    private final VectorStore vectorStore;
    private final MemoRepository memoRepository;
    private final MemoSearchProperties memoSearchProperties;

    // memoIdFilter: null이면 전체 메모가 대상, 아니면 해당 id의 메모만 대상 (비어 있으면 검색 없이 빈 결과)
    public List<Memo> retrieve(Long userId, String query, Collection<Long> memoIdFilter) {
        if (memoIdFilter != null && memoIdFilter.isEmpty()) {
            return List.of();
        }

        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            var filter = memoIdFilter == null
                    ? b.eq("userId", userId)
                    : b.and(b.eq("userId", userId), b.in("memoId", memoIdFilter.toArray()));

            // threshold는 여기서 걸지 않고 topK 후보를 전부 받아서 애플리케이션 레벨에서 거른다.
            // (SearchRequest에 similarityThreshold를 걸면 컷된 후보는 아예 안 돌아와서, 로그로도 못 남기고
            //  나중에 threshold를 재조정할 때 "얼마나 아깝게 컷됐는지"를 알 수 없게 된다.)
            var searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(memoSearchProperties.getVectorTopK())
                    .similarityThresholdAll()
                    .filterExpression(filter.build())
                    .build();

            double threshold = memoSearchProperties.getSemanticSimilarityThreshold();

            // memoId별로 가장 높은 유사도만 사용
            Map<Long, Double> similarityByMemoId = new LinkedHashMap<>();

            vectorStore.similaritySearch(searchRequest).forEach(doc -> {
                Object memoIdObj = doc.getMetadata().get("memoId");
                Object distanceObj = doc.getMetadata().get(DocumentMetadata.DISTANCE.value());
                double similarity = distanceObj instanceof Number distance ? 1 - distance.doubleValue() : 0;
                boolean passed = similarity >= threshold;

                // 향후 threshold 재조정을 위해 통과/탈락 여부와 무관하게 전부 로그로 남긴다 (임시값, 실사용 데이터로 재산정 예정)
                log.debug("[Search][Semantic] userId={} query=\"{}\" memoId={} similarity={} threshold={} passed={}",
                        userId, query, memoIdObj, similarity, threshold, passed);

                if (passed && memoIdObj instanceof Number memoId) {
                    similarityByMemoId.putIfAbsent(memoId.longValue(), similarity);
                }
            });

            // 1위 유사도에서 relativeGap 넘게 낮은 후보는 제외
            double topSimilarity = similarityByMemoId.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double relativeFloor = topSimilarity - memoSearchProperties.getSemanticRelativeGap();

            // threshold를 통과한 후보를 유사도 순으로 "모두" 반환한다.
            // 상위 N개로 자르는 것은 텍스트 검색 결과와 중복 제거한 뒤 서비스 계층에서 처리한다.
            // (여기서 미리 자르면, 상위 후보가 텍스트 결과와 겹칠 때 의미 검색 결과가 통째로 사라진다.)
            List<Long> passedMemoIds = similarityByMemoId.entrySet().stream()
                    .filter(entry -> entry.getValue() >= relativeFloor)
                    .map(Map.Entry::getKey)
                    .toList();

            if (passedMemoIds.isEmpty()) {
                return List.of();
            }

            // findByIdInWithTagsAndNotDeleted는 IN 조회라 반환 순서가 보장되지 않으므로 유사도 순서(passedMemoIds)로 재정렬한다.
            Map<Long, Memo> memoById = memoRepository.findByIdInWithTagsAndNotDeleted(userId, passedMemoIds).stream()
                    .collect(Collectors.toMap(Memo::getId, Function.identity()));

            return passedMemoIds.stream()
                    .map(memoById::get)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.warn("[MemoSearchVectorRetriever] 벡터 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
