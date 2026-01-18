package org.project.domain.ai.rag.B.transform.file;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(30)
public class FileMetadataEnricherTransformer implements DocumentTransformer {

    @Override
    public List<Document> apply(List<Document> documents) {
        documents.forEach(doc -> {
            doc.getMetadata().put("source_type", "FILE");
            doc.getMetadata().putIfAbsent("origin", "memo_file");
        });
        return documents;
    }
}
