package org.project.domain.memo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.global.util.S3Util;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemoEventListener {
    private final S3Util s3Util;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemoDeleted(MemoDeletedEvent event) {
        log.info("메모 삭제 완료 - S3 파일 삭제 시작: memoId={}", event.getMemoId());

        event.getImageKeys().forEach(s3Util::deleteFile);
        event.getFileKeys().forEach(s3Util::deleteFile);

        log.info("S3 파일 삭제 완료: memoId={}", event.getMemoId());
    }

}
