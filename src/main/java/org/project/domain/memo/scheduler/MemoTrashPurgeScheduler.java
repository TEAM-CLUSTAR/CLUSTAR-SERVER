package org.project.domain.memo.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.config.MemoTrashProperties;
import org.project.domain.memo.service.MemoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 휴지통 보존 기간이 지난 메모를 매일 영구 삭제한다.
// 블루그린 전환 중 두 인스턴스에서 겹쳐 돌아도, 이미 지운 메모는 다시 조회되지 않아 결과가 같다(락 불필요).
@Slf4j
@Component
@ConditionalOnProperty(name = "memo.trash.purge-enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class MemoTrashPurgeScheduler {

    private final MemoService memoService;
    private final MemoTrashProperties memoTrashProperties;

    @Scheduled(cron = "${memo.trash.purge-cron}", zone = "Asia/Seoul")
    public void purgeExpiredTrash() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(memoTrashProperties.getRetentionDays());

        try {
            int purged = memoService.purgeExpiredTrash(cutoff, memoTrashProperties.getPurgeBatchSize());
            log.info("휴지통 자동 영구 삭제 완료: cutoff={} purged={}", cutoff, purged);
        } catch (RuntimeException e) {
            log.error("휴지통 자동 영구 삭제 실패: cutoff={}", cutoff, e);
        }
    }
}
