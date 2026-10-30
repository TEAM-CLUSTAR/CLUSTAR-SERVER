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

    // 의미 검색 결과로 최대 몇 개를 반환할지
    @Min(1)
    private int maxMemoResults;

    // 1위 유사도보다 이 값 넘게 낮은 후보는 제외 (threshold를 통과한 후보가 너무 많아지는 것 방지)
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double semanticRelativeGap;

    // 강한 텍스트 일치로 보는 최소 단어 일치 비율 (검색어 전체 구문이 들어 있으면 비율과 무관하게 강한 일치)
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double textStrongTokenCoverage;

    // 강한 텍스트 일치 최대 개수
    @Min(1)
    private int textStrongMaxResults;

    // 약한 텍스트 일치(단어 일부만 일치) 최대 개수, 0이면 반환하지 않음
    @Min(0)
    private int textWeakMaxResults;

    // 최종 검색 결과 전체 상한
    @Min(1)
    private int maxTotalResults;

    /**
     * 검색 모달 [입력 완료 전]의 "최근 열람한 메모" 목록에 노출할 개수.
     */
    @Min(1)
    private int recentViewedLimit;
}
