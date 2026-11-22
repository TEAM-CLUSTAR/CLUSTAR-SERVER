package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.event.MemoPurgedEvent;
import org.project.domain.memo.repository.VectorStoreRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 메모 영구 삭제 시 그 메모들의 모든 벡터(text/image/file)를 정리한다.
// 휴지통 이동 때는 복원에 대비해 벡터를 남겨 두고, 검색/추천/AI 쪽에서 휴지통 메모를 걸러낸다.
// 실패는 로그만 남긴다(커밋 후 실행이라 롤백 불가, 메모가 없으니 조회 결과에는 노출되지 않음).
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoPurgeVectorCleanupListener {

    private final VectorStoreRepository vectorStoreRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoPurgedEvent event) {
        try {
            vectorStoreRepository.deleteByMemoIds(event.memoIds());
            log.info("메모 영구 삭제 - 벡터 정리 완료: memoIds={}", event.memoIds());
        } catch (Exception e) {
            log.error("메모 영구 삭제 - 벡터 정리 실패: memoIds={}", event.memoIds(), e);
        }
    }
}
