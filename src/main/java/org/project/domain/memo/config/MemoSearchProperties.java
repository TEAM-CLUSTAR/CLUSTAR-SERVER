package org.project.domain.memo.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@ConfigurationProperties(prefix = "memo.search")
public class MemoSearchProperties {

    /**
     * 검색어와 후보 메모가 얼마나 유사해야 의미검색 결과로 인정할지.
     * 이 값 미만이면 topK 안에 들어도 결과에서 제외한다(기본 SearchRequest는 이 필터가 없어 전부 통과시킨다).
     */
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double semanticSimilarityThreshold;

    /**
     * 벡터스토어에서 몇 개의 후보를 받아와 threshold 필터링할지.
     */
    @Min(1)
    private int vectorTopK;

    /**
     * threshold를 통과한 후보 중 최종적으로 몇 개를 반환할지.
     */
    @Min(1)
    private int maxMemoResults;

    /**
     * 검색 모달 [입력 완료 전]의 "최근 열람한 메모" 목록에 노출할 개수.
     */
    @Min(1)
    private int recentViewedLimit;
}
