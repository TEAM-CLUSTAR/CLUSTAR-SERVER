package org.project.domain.ai.rag.F.augment;

import org.project.domain.ai.rag.D.query.RagQuery;
import org.springframework.ai.document.Document;

import java.util.List;

public interface RagAugmenter {

    RagPrompt augment(
            RagQuery query,
            List<Document> documents
    );
}
