package org.project.domain.ai.rag.E.retrieve;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultMemoFileRetriever implements MemoFileRetriever {

    private final VectorStore vectorStore;

    @Override
    public List<Document> retrieve(RagQuery query) {

        SearchRequest request = SearchRequest.builder()
                .query(query.userPrompt())
                .topK(5)
                .filterExpression(buildFilter(query))
                .build();

        return vectorStore.similaritySearch(request);
    }

    private Filter.Expression buildFilter(RagQuery query) {

        FilterExpressionBuilder b = new FilterExpressionBuilder();

        // 1️⃣ 기본 조건
        FilterExpressionBuilder.Op baseOp = b.and(
                b.eq("type", "MEMO_FILE"),
                b.eq("userId", query.userId())
        );

        // 2️⃣ memoIds IN 조건 (선택)
        if (query.memoIds() != null && !query.memoIds().isEmpty()) {

            FilterExpressionBuilder.Op memoIdOp =
                    b.in("memoId", query.memoIds().stream()
                            .map(id -> (Object) id)
                            .toList()
                    );

            return b.and(baseOp, memoIdOp).build();
        }

        return baseOp.build();
    }
}
