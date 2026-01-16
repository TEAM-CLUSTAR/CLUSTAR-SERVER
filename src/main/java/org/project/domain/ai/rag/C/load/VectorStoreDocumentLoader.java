package org.project.domain.ai.rag.C.load;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreDocumentLoader {

    private final VectorStore vectorStore;

    public void load(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("[VectorStore] No documents to load");
            return;
        }

        log.info("[VectorStore] Loading {} documents (chunks)", documents.size());

        documents.stream()
                .limit(2)
                .forEach(doc -> log.debug(
                        "[Chunk Preview] id={}, length={}, metadata={}",
                        doc.getId(),
                        doc.getText() != null ? doc.getText().length() : -1,
                        doc.getMetadata()
                ));

        vectorStore.write(documents);

        log.info("[VectorStore] Load completed");
    }
}

