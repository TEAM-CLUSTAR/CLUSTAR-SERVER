package org.project.domain.ai.rag.B.transform.file;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(10)
public class FileTextCleanupTransformer implements DocumentTransformer {

    @Override
    public List<Document> apply(List<Document> documents) {
        return documents.stream()
                .map(doc -> {
                    String cleaned = doc.getText()
                            .replaceAll("\\s+", " ")
                            .replaceAll("(?i)page \\d+", "")
                            .trim();

                    return new Document(cleaned, doc.getMetadata());
                })
                .collect(Collectors.toList());
    }
}
