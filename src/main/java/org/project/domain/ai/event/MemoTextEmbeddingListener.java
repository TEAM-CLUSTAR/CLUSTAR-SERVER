package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.A.extract.MemoDocumentReader;
import org.project.domain.ai.rag.B.transform.text.MemoTextTransformer;
import org.project.domain.ai.rag.C.load.VectorStoreDocumentLoader;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.event.MemoTextCreatedEvent;
import org.project.domain.memo.repository.MemoRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemoTextEmbeddingListener {

    private final MemoRepository memoRepository;

    private final MemoDocumentReader memoDocumentReader;      // Extract
    private final MemoTextTransformer memoTextTransformer;    // Transform
    private final VectorStoreDocumentLoader vectorStoreDocumentLoader; // Load

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoTextCreatedEvent event) {

        // 1️⃣ Extract
        Memo memo = memoRepository.findByIdWithUserAndNotDeleted(event.memoId())
                .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));

        List<Document> extractedDocuments =
                memoDocumentReader.readText(memo);

        if (extractedDocuments.isEmpty()) {
            return;
        }

        // 2️⃣ Transform
        List<Document> transformedDocuments =
                memoTextTransformer.transform(extractedDocuments);

        if (transformedDocuments.isEmpty()) {
            return;
        }

        // 3️⃣ Load
        vectorStoreDocumentLoader.load(transformedDocuments);
    }
}
