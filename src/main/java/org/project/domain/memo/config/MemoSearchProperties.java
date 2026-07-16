package org.project.domain.memo.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "memo.search")
public class MemoSearchProperties {

    /**
     * 검색어와 후보 메모가 얼마나 유사해야 의미검색 결과로 인정할지.
     * 이 값 미만이면 topK 안에 들어도 결과에서 제외한다(기본 SearchRequest는 이 필터가 없어 전부 통과시킨다).
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double semanticSimilarityThreshold;
}
