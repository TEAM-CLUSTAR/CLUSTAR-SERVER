package org.project.domain.ai.rag.A.extract;

import lombok.RequiredArgsConstructor;
import org.project.domain.memo.entity.Memo;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MemoDocumentReader {

    public List<Document> readText(Memo memo) {

        if (memo.getContent() == null || memo.getContent().isBlank()) {
            return List.of();
        }

        Document document = new Document(
                memo.getContent(),
                Map.of(
                        "type", RagDocumentType.MEMO_TEXT.name(),
                        "memoId", memo.getId(),
                        "userId", memo.getUser().getId(),
                        "title", memo.getTitle(),
                        "createdAt", Instant.now().toString()
                )
        );

        return List.of(document);
    }
}
