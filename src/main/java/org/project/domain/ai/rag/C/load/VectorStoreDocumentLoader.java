package org.project.domain.ai.rag.C.load;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreDocumentLoader {

    private final VectorStore vectorStore;

    // 재임베딩 스킵 판단 기준(어떤 모델로 임베딩됐는지)에 쓰인다 - 이 값이 바뀌면(모델 교체) 기존 벡터는 자동으로 "구버전" 취급된다.
    @Value("${spring.ai.google.genai.embedding.text.options.model}")
    private String embeddingModel;

    public void load(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.debug("[VectorStore] No documents to load");
            return;
        }

        // 이 벡터가 실제로 (재)임베딩된 시각/모델 - 신규 생성/재임베딩 모두 이 지점을 거치므로 여기서 한 번만 찍는다
        String embeddedAt = LocalDateTime.now().toString();
        documents.forEach(doc -> {
            doc.getMetadata().put("embeddedAt", embeddedAt);
            doc.getMetadata().put("embeddingModel", embeddingModel);
        });

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

