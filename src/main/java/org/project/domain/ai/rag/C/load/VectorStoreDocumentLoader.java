package org.project.domain.ai.rag.C.load;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VectorStoreDocumentLoader {

    private final VectorStore vectorStore;

    public void load(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        vectorStore.write(documents);
    }
}
