package org.project.domain.ai.rag.F.augment;

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
                .collect(Collectors.joining("\n\n"));
    }

    private String formatDocument(Document document) {

        if (!document.isText()) {
            return ""; // 또는 media 처리
        }

        String source = String.valueOf(
                document.getMetadata().getOrDefault("rag_source", "unknown")
        );

        String fileName = String.valueOf(
                document.getMetadata().getOrDefault("fileName", "")
        );

        String sourceLabel = fileName.isBlank()
                ? source
                : source + " | " + fileName;

        return """
        [SOURCE: %s]
        %s
        """.formatted(
                sourceLabel,
                document.getText()
        );
    }

}
