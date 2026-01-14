package org.project.domain.memo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.s3.entity.S3DeletionFailure;
import org.project.domain.s3.repository.S3DeletionFailureRepository;
import org.project.global.util.S3Util;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3DeletionHandler {
    private final S3Util s3Util;
    private final S3DeletionFailureRepository failureRepository;

    // 호출될 때마다 무조건 새 트랜잭션을 열어서 실패 기록을 DB에 커밋함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOrRecord(String key, Long memoId, String fileType) {
        try {
            s3Util.deleteFile(key);
            log.info("S3 삭제 성공: {}", key);
        } catch (Exception e) {
            log.error("S3 삭제 실패 - DB 기록 저장: {}", key);
            S3DeletionFailure failure = S3DeletionFailure.builder()
                    .s3Key(key)
                    .memoId(memoId)
                    .fileType(fileType)
                    .failedAt(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .isResolved(false)
                    .build();
            failureRepository.save(failure);
        }
    }
}
