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

/**
 * 메모 수정으로 제거된 이미지/파일 첨부의 벡터를 정리한다.
 * imageId/fileId 단위로 벡터 document id를 찾아 삭제한다(첨부는 벡터 metadata에 개별 id가 심겨 있음).
 * 실패는 로그만 남기고 삼킨다(발행 트랜잭션은 이미 커밋됨). 벡터 삭제 실패는 검색 정확도에만 영향.
 */
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
