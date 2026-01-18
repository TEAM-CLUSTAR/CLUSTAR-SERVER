package org.project.domain.ai.rag.E.retrieve.text;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultMemoContentRetriever implements MemoContentRetriever {

    private final VectorStore vectorStore;

    @Override
    public List<Document> retrieve(RagQuery query) {

        /*
         * Memo TEXT Retrieval
         *
         * metadata 기준:
         * - userId
         * - memoId
         * - type = MEMO_TEXT
         */

        if (query.memoIds() == null || query.memoIds().isEmpty()) {
            throw new AiException(AiErrorCode.EMPTY_MEMO_IDS);
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query.userPrompt())
                .topK(5)
                .filterExpression(buildFilter(query))
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * VectorStore metadata filter 생성
     */
    private String buildFilter(RagQuery query) {
        return """
               userId == %d
               && type == "MEMO_TEXT"
               && memoId in [%s]
               """.formatted(
                query.userId(),
                query.memoIds().stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("")
        );
    }
}
