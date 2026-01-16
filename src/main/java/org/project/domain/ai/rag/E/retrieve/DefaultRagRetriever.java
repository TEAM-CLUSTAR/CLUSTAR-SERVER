package org.project.domain.ai.rag.E.retrieve;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.D.query.RagQuery;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultRagRetriever implements RagRetriever {

    private final MemoContentRetriever memoContentRetriever;
    private final MemoImageRetriever memoImageRetriever;
    private final MemoFileRetriever memoFileRetriever;

    @Override
    public List<Document> retrieve(RagQuery query) {

        List<Document> results = new ArrayList<>();

        // 1️⃣ Memo Text
        results.addAll(
                safeRetrieve(() -> memoContentRetriever.retrieve(query))
        );

        // 2️⃣ Memo Image
        results.addAll(
                safeRetrieve(() -> memoImageRetriever.retrieve(query))
        );

        // 3️⃣ Memo File
        results.addAll(
                safeRetrieve(() -> memoFileRetriever.retrieve(query))
        );

        return results;
    }

    private List<Document> safeRetrieve(Supplier<List<Document>> supplier) {
        try {
            List<Document> documents = supplier.get();
            return documents != null ? documents : List.of();
        } catch (Exception e) {
            // ❗ Retriever 하나 실패해도 전체 파이프라인은 유지
            return List.of();
        }
    }
}
