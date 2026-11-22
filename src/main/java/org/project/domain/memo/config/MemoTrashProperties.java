package org.project.domain.memo.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "memo.trash")
public class MemoTrashProperties {

    // 휴지통 보존 기간(일). 이 기간이 지난 메모는 자동 영구 삭제된다.
    @Min(1)
    private int retentionDays;

    // 자동 영구 삭제 시 한 트랜잭션에서 지울 메모 수
    @Min(1)
    private int purgeBatchSize;

    // 휴지통 목록 한 번에 조회할 수 있는 최대 개수 (size 파라미터 상한)
    @Min(1)
    private int maxPageSize;
}
