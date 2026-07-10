package org.project.domain.memo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "memo.recommendation")
public class MemoRecommendationProperties {

    /**
     * 선택된 메모들이 서로 얼마나 응집돼 있어야 추천을 시도할지 (Gate 1).
     * 선택 메모 쌍별 코사인 유사도 평균이 이 값 미만이면 후보 검색 없이 빈 결과를 반환한다.
     */
    private double cohesionThreshold;

    /**
     * 후보 메모가 선택 평균 벡터와 얼마나 유사해야 추천 후보로 인정할지 (Gate 2, 선택 2개 이상).
     * 평균 벡터는 여러 임베딩을 합쳐 노이즈를 상쇄시키므로 단일 벡터 비교보다 유사도가 전반적으로 높게 나온다.
     */
    private double candidateSimilarityThreshold;

    /**
     * 메모를 1개만 선택했을 때 적용하는 Gate 2 임계값. 평균화 효과가 없어 원본 벡터끼리 직접 비교되므로
     * candidateSimilarityThreshold보다 구조적으로 낮게 잡아야 한다.
     */
    private double singleSelectionSimilarityThreshold;
}
