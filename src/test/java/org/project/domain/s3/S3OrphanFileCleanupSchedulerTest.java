package org.project.domain.s3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.memo.repository.MemoFileRepository;
import org.project.domain.memo.repository.MemoImageRepository;
import org.project.global.util.S3Util;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3OrphanFileCleanupSchedulerTest {

    @Mock private S3Util s3Util;
    @Mock private MemoImageRepository memoImageRepository;
    @Mock private MemoFileRepository memoFileRepository;

    @Test
    void deletesOnlyUnreferencedObjectsOlderThanGracePeriod() {
        S3OrphanFileCleanupScheduler scheduler = new S3OrphanFileCleanupScheduler(
                s3Util, memoImageRepository, memoFileRepository);
        S3Object referenced = object("memo-image/1/referenced.png", Instant.now().minusSeconds(1_000));
        S3Object orphan = object("memo-file/1/orphan.pdf", Instant.now().minusSeconds(1_000));
        S3Object recent = object("memo-image/1/recent.png", Instant.now());

        given(memoImageRepository.findAllImageS3Keys()).willReturn(List.of(referenced.key()));
        given(memoFileRepository.findAllFileS3Keys()).willReturn(List.of());
        given(s3Util.listObjects("memo-image/")).willReturn(List.of(referenced, recent));
        given(s3Util.listObjects("memo-file/")).willReturn(List.of(orphan));

        scheduler.deleteOrphanFiles();

        verify(s3Util).deleteFile(orphan.key());
        verify(s3Util, never()).deleteFile(referenced.key());
        verify(s3Util, never()).deleteFile(recent.key());
    }

    private S3Object object(String key, Instant lastModified) {
        return S3Object.builder().key(key).lastModified(lastModified).build();
    }
}
