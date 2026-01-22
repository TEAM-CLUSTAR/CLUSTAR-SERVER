package org.project.domain.ai.rag.F.augment.context;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagContextBuilder {

    public String build(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        return documents.stream()
                .map(this::formatDocument)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private String formatDocument(Document document) {

        String type = String.valueOf(document.getMetadata().get("type"));

        // 메모 계열만 허용
        if (!type.startsWith("MEMO_")) {
            return "";
        }

        return """
        [MEMO]
        %s
        """.formatted(
                document.getText().trim()
        );
    }
}
