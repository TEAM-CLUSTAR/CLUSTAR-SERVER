package org.project.domain.ai.rag.B.transform.image;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoImageDocumentTransformer {

    private final List<DocumentTransformer> transformers;

    public MemoImageDocumentTransformer(
            @Qualifier("imageTransformers")
            List<DocumentTransformer> transformers
    ) {
        this.transformers = transformers;
    }

    public List<Document> transform(List<Document> documents) {
        List<Document> current = documents;
        for (DocumentTransformer transformer : transformers) {
            current = transformer.apply(current);
        }
        return current;
    }
}
