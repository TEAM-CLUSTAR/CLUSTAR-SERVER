package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.event.MemoDeletedEvent;
import org.project.domain.memo.repository.VectorStoreRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 메모 삭제 시 그 메모의 모든 벡터(text/image/file)를 정리한다.
// (기존엔 S3만 지우고 벡터는 남아 유령 벡터가 누적되던 문제 대응)
// 실패는 로그만 남긴다(커밋 후 실행이라 롤백 불가, 검색 정확도에만 영향). 정합성 배치가 2차 안전망.
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoDeleteVectorCleanupListener {

    private final VectorStoreRepository vectorStoreRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoDeletedEvent event) {
        try {
            vectorStoreRepository.deleteByMemoId(event.getMemoId());
            log.info("메모 삭제 - 벡터 정리 완료: memoId={}", event.getMemoId());
        } catch (Exception e) {
            log.error("메모 삭제 - 벡터 정리 실패: memoId={}", event.getMemoId(), e);
        }
    }
}
