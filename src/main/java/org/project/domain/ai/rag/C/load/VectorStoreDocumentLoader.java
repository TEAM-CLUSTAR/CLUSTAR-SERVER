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
                .limit(10)
                .forEach(doc -> {
                    String text = doc.getText();
                    String preview = (text == null)
                            ? "null"
                            : text.substring(0, Math.min(100, text.length()));

                    log.info(
                            "[Chunk Preview] id={}, length={}, preview=\"{}\", metadata={}",
                            doc.getId(),
                            text != null ? text.length() : -1,
                            preview.replaceAll("\\s+", " "), // 줄바꿈 정리
                            doc.getMetadata()
                    );
                });


        vectorStore.write(documents);

        log.info("[VectorStore] Load completed");
    }
}

