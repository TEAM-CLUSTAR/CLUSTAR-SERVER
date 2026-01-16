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

//    private String formatDocument(Document document) {
//        return """
//            [SOURCE]
//            %s
//            """.formatted(document.getFormattedContent());
//    }

    private String formatDocument(Document document) {
        String source = String.valueOf(
                document.getMetadata().getOrDefault("source", "unknown")
        );

        return """
            [SOURCE: %s]
            %s
            """.formatted(
                source,
                document.getFormattedContent()
        );
    }

}
