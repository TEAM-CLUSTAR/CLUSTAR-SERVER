package org.project.domain.ai.rag.F.augment.context;

import org.project.domain.ai.rag.F.augment.dto.RagContextResult;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagContextBuilder {

    public RagContextResult build(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return new RagContextResult("", 0);
        }

        String context = documents.stream()
                .map(this::formatDocument)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));

        int pureTextLength = documents.stream()
                .map(this::extractPureText)
                .filter(s -> !s.isBlank())
                .mapToInt(String::length)
                .sum();

        return new RagContextResult(context, pureTextLength);
    }

    private String formatDocument(Document document) {
        String type = String.valueOf(document.getMetadata().get("type"));

        // 메모 계열만 허용
        if (!type.startsWith("MEMO_")) {
            return "";
        }

        String text = document.getText();
        if (text == null || text.isBlank()) {
            return "";
        }

        return """
        [MEMO]
        %s
        """.formatted(text.trim());
    }

    private String extractPureText(Document document) {
        String type = String.valueOf(document.getMetadata().get("type"));

        if (!type.startsWith("MEMO_")) {
            return "";
        }

        String text = document.getText();
        return text == null ? "" : text.trim();
    }
}
