package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.A.extract.MemoFileDocumentReader;
import org.project.domain.ai.rag.B.transform.file.MemoFileDocumentTransformer;
import org.project.domain.ai.rag.C.load.VectorStoreDocumentLoader;
import org.project.domain.memo.event.MemoFileCreatedEvent;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemoFileEmbeddingListener {

    private final MemoFileDocumentReader memoFileDocumentReader;
    private final MemoFileDocumentTransformer memoFileDocumentTransformer;
    private final VectorStoreDocumentLoader vectorStoreDocumentLoader;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoFileCreatedEvent event) {

        // 1️⃣ Extract
        List<Document> documents =
                memoFileDocumentReader.read(
                        event.memoId(),
                        event.memoFileIds(),
                        event.userId()
                );

        // 2️⃣ Transform
        List<Document> transformed =
                memoFileDocumentTransformer.transform(documents);

        // 3️⃣ Load
        vectorStoreDocumentLoader.load(transformed);
    }
}
