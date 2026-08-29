package org.project.domain.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.global.util.S3Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3OrphanFileCleanupScheduler {

    private static final Duration ORPHAN_GRACE_PERIOD = Duration.ofMinutes(10);
    private static final List<String> MANAGED_PREFIXES = List.of("memo-image/", "memo-file/");

    private final S3Util s3Util;
    private final MemoImageRepository memoImageRepository;
    private final MemoFileRepository memoFileRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteOrphanFiles() {
        Set<String> referencedKeys = new HashSet<>(memoImageRepository.findAllImageS3Keys());
        referencedKeys.addAll(memoFileRepository.findAllFileS3Keys());
        Instant cutoff = Instant.now().minus(ORPHAN_GRACE_PERIOD);

        MANAGED_PREFIXES.stream()
                .flatMap(prefix -> s3Util.listObjects(prefix).stream())
                .filter(object -> !referencedKeys.contains(object.key()))
                .filter(object -> object.lastModified() != null && object.lastModified().isBefore(cutoff))
                .forEach(object -> deleteOrLog(object.key()));
    }

    private void deleteOrLog(String key) {
        try {
            s3Util.deleteFile(key);
            log.info("미참조 S3 객체 삭제 성공 - Key: {}", key);
        } catch (RuntimeException e) {
            log.error("미참조 S3 객체 삭제 실패 - Key: {}", key, e);
        }
    }
}
