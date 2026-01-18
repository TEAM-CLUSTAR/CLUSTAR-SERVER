package org.project.domain.ai.rag.E.retrieve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.project.domain.ai.rag.E.retrieve.file.MemoFileRetriever;
import org.project.domain.ai.rag.E.retrieve.image.MemoImageRetriever;
import org.project.domain.ai.rag.E.retrieve.text.MemoContentRetriever;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRagRetriever implements RagRetriever {

    private final MemoContentRetriever memoContentRetriever;
    private final MemoImageRetriever memoImageRetriever;
    private final MemoFileRetriever memoFileRetriever;

    @Override
    public List<Document> retrieve(RagQuery query) {

        List<Document> results = new ArrayList<>();

        results.addAll(
                safeRetrieve(
                        () -> memoContentRetriever.retrieve(query),
                        "MEMO_TEXT",
                        query
                )
        );

        results.addAll(
                safeRetrieve(
                        () -> memoImageRetriever.retrieve(query),
                        "MEMO_IMAGE",
                        query
                )
        );

        results.addAll(
                safeRetrieve(
                        () -> memoFileRetriever.retrieve(query),
                        "MEMO_FILE",
                        query
                )
        );

        return results;
    }


    private List<Document> safeRetrieve(
            Supplier<List<Document>> supplier,
            String retrieverName,
            RagQuery query
    ) {
        try {
            List<Document> documents = supplier.get();
            return documents != null ? documents : List.of();
        } catch (Exception e) {
            log.warn(
                    "[RAG][RETRIEVE_FAIL] retriever={} userId={} memoIds={}",
                    retrieverName,
                    query.userId(),
                    query.memoIds(),
                    e
            );
            return List.of();
        }
    }

}
