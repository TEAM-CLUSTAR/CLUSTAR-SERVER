package org.project.domain.ai.rag.B.transform;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemoFileDocumentTransformer {

    private final List<DocumentTransformer> transformers;

    public List<Document> transform(List<Document> documents) {
        List<Document> current = documents;
        for (DocumentTransformer transformer : transformers) {
            current = transformer.apply(current);
        }
        return current;
    }
}
