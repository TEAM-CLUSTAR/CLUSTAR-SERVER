package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.ai.rag.A.extract.MemoDocumentReader;
import org.project.domain.ai.rag.A.extract.RagDocumentType;
import org.project.domain.ai.rag.B.transform.text.MemoTextTransformer;
import org.project.domain.ai.rag.C.load.VectorStoreDocumentLoader;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.event.MemoTextUpdatedEvent;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.memo.repository.VectorStoreRepository;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * 메모 제목/본문 수정 시 텍스트 벡터를 재색인한다.
 * <p>
 * "새 벡터 적재 성공 → 그 뒤 기존 벡터 삭제" 순서를 지킨다.
 * 적재 전에 기존 document id를 먼저 캡처해두는 이유: 적재는 새 UUID로 들어가므로,
 * 옛 id만 정확히 지우기 위해서다. 적재가 실패하면 기존 벡터를 남겨(구버전이라도 검색 가능) 실패 기록만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoTextUpdateEmbeddingListener {

    private final MemoRepository memoRepository;
    private final VectorStoreRepository vectorStoreRepository;

    private final MemoDocumentReader memoDocumentReader;      // Extract
    private final MemoTextTransformer memoTextTransformer;    // Transform
    private final VectorStoreDocumentLoader vectorStoreDocumentLoader; // Load
    private final EmbeddingFailureHandler embeddingFailureHandler;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoTextUpdatedEvent event) {
        try {
            // 0️⃣ 지울 대상(기존 텍스트 벡터) id를 먼저 캡처
            List<UUID> oldDocIds = vectorStoreRepository
                    .findDocumentIdsByMemoIdAndType(event.memoId(), RagDocumentType.MEMO_TEXT.name());

            // 1️⃣ Extract
            Memo memo = memoRepository.findByIdWithUserAndNotDeleted(event.memoId())
                    .orElseThrow(() -> new MemoException(MemoErrorCode.MEMO_NOT_FOUND));

            List<Document> extractedDocuments = memoDocumentReader.readText(memo);

            if (extractedDocuments.isEmpty()) {
                // 본문이 비게 수정된 경우 등 — 새로 넣을 게 없으면 기존 벡터만 정리
                vectorStoreRepository.deleteByIds(oldDocIds);
                return;
            }

            // 2️⃣ Transform
            List<Document> transformedDocuments = memoTextTransformer.transform(extractedDocuments);

            if (transformedDocuments.isEmpty()) {
                vectorStoreRepository.deleteByIds(oldDocIds);
                return;
            }

            // 3️⃣ Load (새 벡터 적재)
            vectorStoreDocumentLoader.load(transformedDocuments);

            // 4️⃣ 적재 성공 후 기존 벡터 삭제
            vectorStoreRepository.deleteByIds(oldDocIds);
        } catch (Exception e) {
            log.error("텍스트 재임베딩 실패: memoId={}", event.memoId(), e);
            embeddingFailureHandler.record(event.memoId(), "text", e);
        }
    }
}
