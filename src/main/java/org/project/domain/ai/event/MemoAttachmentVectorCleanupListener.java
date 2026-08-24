package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.event.MemoAttachmentsRemovedEvent;
import org.project.domain.memo.repository.VectorStoreRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 메모 수정으로 제거된 첨부의 벡터를 imageId/fileId 단위로 찾아 삭제.
// 실패는 로그만 남긴다(커밋 후 실행이라 롤백 불가, 검색 정확도에만 영향).
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoAttachmentVectorCleanupListener {

    private final VectorStoreRepository vectorStoreRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoAttachmentsRemovedEvent event) {
        try {
            List<UUID> docIds = new ArrayList<>();

            for (Long imageId : event.removedImageIds()) {
                docIds.addAll(vectorStoreRepository.findDocumentIdsByImageId(imageId));
            }
            for (Long fileId : event.removedFileIds()) {
                docIds.addAll(vectorStoreRepository.findDocumentIdsByFileId(fileId));
            }

            vectorStoreRepository.deleteByIds(docIds);
            log.info("첨부 벡터 정리 완료: memoId={}, 삭제 벡터 수={}", event.memoId(), docIds.size());
        } catch (Exception e) {
            log.error("첨부 벡터 정리 실패: memoId={}", event.memoId(), e);
        }
    }
}
