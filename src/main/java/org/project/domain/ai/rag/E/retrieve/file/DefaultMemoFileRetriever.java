package org.project.domain.ai.rag.E.retrieve.file;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.A.extract.RagDocumentType;
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

        List<Document> documents = vectorStore.similaritySearch(request);

        // source metadata 추가
        documents.forEach(doc ->
                doc.getMetadata().put("rag_source", RagDocumentType.MEMO_FILE.name())
        );

        return documents;
    }

    private Filter.Expression buildFilter(RagQuery query) {

        FilterExpressionBuilder b = new FilterExpressionBuilder();

        FilterExpressionBuilder.Op baseOp = b.and(
                b.eq("type", RagDocumentType.MEMO_FILE.name()),
                b.eq("userId", query.userId())
        );

        if (query.memoIds() != null && !query.memoIds().isEmpty()) {
            return b.and(
                    baseOp,
                    b.in(
                            "memoId",
                            query.memoIds().stream()
                                    .map(id -> (Object) id)
                                    .toList()
                    )
            ).build();
        }

        return baseOp.build();
    }

}
