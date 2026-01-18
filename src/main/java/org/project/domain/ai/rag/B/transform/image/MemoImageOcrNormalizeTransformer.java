package org.project.domain.ai.rag.B.transform.image;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoImageOcrNormalizeTransformer implements DocumentTransformer {

    @Override
    public List<Document> apply(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {

            // Text Document만 처리
            if (!doc.isText()) {
                continue;
            }

            String text = doc.getText();

            if (text == null || text.isBlank()) {
                continue; // OCR 실패 or 무의미 텍스트
            }

            String enriched = """
            [IMAGE OCR CONTENT]
            %s
            """.formatted(text);

            Document transformed = doc.mutate()
                    .text(enriched)
                    .build();

            result.add(transformed);
        }

        return result;
    }
}
