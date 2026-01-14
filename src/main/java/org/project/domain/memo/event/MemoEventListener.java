package org.project.domain.memo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoEventListener {
    private final S3DeletionHandler s3DeletionHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemoDeleted(MemoDeletedEvent event) {
        log.info("메모 삭제 완료 - S3 파일 삭제 시작: memoId={}", event.getMemoId());

        event.getImageKeys().forEach(key ->
                s3DeletionHandler.deleteOrRecord(key, event.getMemoId(), "image")
        );
        event.getFileKeys().forEach(key ->
                s3DeletionHandler.deleteOrRecord(key, event.getMemoId(), "file")
        );

        log.info("S3 파일 삭제 완료: memoId={}", event.getMemoId());
    }

}
