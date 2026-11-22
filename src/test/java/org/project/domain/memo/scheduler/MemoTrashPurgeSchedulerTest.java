package org.project.domain.memo.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.memo.config.MemoTrashProperties;
import org.project.domain.memo.service.MemoService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("휴지통 자동 영구 삭제 스케줄러")
class MemoTrashPurgeSchedulerTest {

    @Mock
    private MemoService memoService;

    private MemoTrashPurgeScheduler scheduler;

    @BeforeEach
    void setUp() {
        MemoTrashProperties properties = new MemoTrashProperties();
        properties.setRetentionDays(30);
        properties.setPurgeBatchSize(100);
        scheduler = new MemoTrashPurgeScheduler(memoService, properties);
    }

    @Test
    @DisplayName("보존 기간(30일) 이전 시각을 기준으로 설정된 배치 크기만큼씩 정리를 요청한다")
    void purgesWithRetentionCutoff() {
        scheduler.purgeExpiredTrash();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(memoService).purgeExpiredTrash(cutoff.capture(), eq(100));
        assertThat(cutoff.getValue()).isCloseTo(LocalDateTime.now().minusDays(30), within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("정리 중 예외가 나도 스케줄러 스레드로 전파하지 않는다")
    void swallowsFailure() {
        given(memoService.purgeExpiredTrash(any(), anyInt())).willThrow(new IllegalStateException("db down"));

        assertThatCode(() -> scheduler.purgeExpiredTrash()).doesNotThrowAnyException();
    }
}
