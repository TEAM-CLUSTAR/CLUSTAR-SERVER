package org.project.domain.ai.rag.C.load;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

        // 이 벡터가 실제로 (재)임베딩된 시각 - 신규 생성/재임베딩 모두 이 지점을 거치므로 여기서 한 번만 찍는다
        String embeddedAt = LocalDateTime.now().toString();
        documents.forEach(doc -> doc.getMetadata().put("embeddedAt", embeddedAt));

        log.info("[VectorStore] Loading {} documents (chunks)", documents.size());

        documents.stream()
                .limit(10)
                .forEach(doc -> {
                    String text = doc.getText();
                    String preview = (text == null)
                            ? "null"
                            : text.substring(0, Math.min(100, text.length()));

                    log.debug(
                            "[Chunk Preview] id={}, length={}, preview=\"{}\", metadata={}",
                            doc.getId(),
                            text != null ? text.length() : -1,
                            preview.replaceAll("\\s+", " "), // 줄바꿈 정리
                            doc.getMetadata()
                    );
                });


        try {
            vectorStore.write(documents);
            log.info("[VectorStore] Load completed");
        } catch (Exception e) {
            log.error("[VectorStore] Failed to load {} documents: {}", documents.size(), e.getMessage(), e);
            throw e;
        }
    }
}

