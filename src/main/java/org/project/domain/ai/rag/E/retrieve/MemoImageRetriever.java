package org.project.domain.ai.rag.E.retrieve;

import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.springframework.ai.document.Document;

import java.util.List;

public interface MemoImageRetriever {
    List<Document> retrieve(RagQuery query);
}
