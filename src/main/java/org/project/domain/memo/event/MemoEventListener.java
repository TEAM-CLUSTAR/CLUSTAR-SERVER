package org.project.domain.memo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoEventListener {
    private final S3DeletionHandler s3DeletionHandler;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemoPurged(MemoPurgedEvent event) {
        log.info("메모 영구 삭제 완료 - S3 파일 삭제 시작: memoIds={}", event.memoIds());

        for (MemoPurgedEvent.PurgedMemo memo : event.memos()) {
            memo.imageKeys().forEach(key -> deleteQuietly(key, memo.memoId(), "image"));
            memo.fileKeys().forEach(key -> deleteQuietly(key, memo.memoId(), "file"));
        }

        log.info("S3 파일 삭제 완료: memoIds={}", event.memoIds());
    }

    // 한 키의 삭제와 실패 기록이 모두 실패해도(예: DB 장애) 같은 묶음의 나머지 키는 계속 처리한다.
    // 이렇게 남은 S3 객체는 DB 참조가 없으므로 S3 미참조 정리 스케줄러가 다시 지운다.
    private void deleteQuietly(String key, Long memoId, String fileType) {
        try {
            s3DeletionHandler.deleteOrRecord(key, memoId, fileType);
        } catch (Exception e) {
            log.error("S3 삭제와 실패 기록 모두 실패 - 미참조 정리로 처리 예정: memoId={} key={}", memoId, key, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAttachmentsRemoved(MemoAttachmentsRemovedEvent event) {
        log.info("메모 수정 - 제거된 첨부 S3 삭제 시작: memoId={}", event.memoId());

        event.removedImageKeys().forEach(key ->
                s3DeletionHandler.deleteOrRecord(key, event.memoId(), "image")
        );
        event.removedFileKeys().forEach(key ->
                s3DeletionHandler.deleteOrRecord(key, event.memoId(), "file")
        );

        log.info("메모 수정 - 제거된 첨부 S3 삭제 완료: memoId={}", event.memoId());
    }

}
