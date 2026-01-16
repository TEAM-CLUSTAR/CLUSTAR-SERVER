package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.A.extract.MemoImageDocumentReader;
import org.project.domain.ai.rag.B.transform.MemoImageDocumentTransformer;
import org.project.domain.ai.rag.C.load.VectorStoreDocumentLoader;
import org.project.domain.memo.event.MemoImageCreatedEvent;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemoImageEmbeddingListener {

    private final MemoImageDocumentReader memoImageDocumentReader;
    private final MemoImageDocumentTransformer memoImageDocumentTransformer;
    private final VectorStoreDocumentLoader vectorStoreDocumentLoader;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoImageCreatedEvent event) {

        // 1️⃣ Extract
        List<Document> documents =
                memoImageDocumentReader.read(
                        event.memoId(),
                        event.memoImageIds(),
                        event.userId()
                );

        // 2️⃣ Transform
        List<Document> transformed =
                memoImageDocumentTransformer.transform(documents);

        // 3️⃣ Load
        vectorStoreDocumentLoader.load(transformed);
    }
}

