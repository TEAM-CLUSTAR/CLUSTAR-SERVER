package org.project.domain.review.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

// AI 검토 튜닝값. 실사용 데이터를 보며 조정할 값이라 yml로 뺀다
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "review")
public class ReviewProperties {

    // 검토를 시작할 수 있는 최소 본문 길이(제목 제외, 공백 포함)
    @Min(1)
    private int minContentLength;

    // 한 번 검토할 때 만들 최대 빈틈 카드 수
    @Min(1)
    private int maxFindings;

    // 답 하나의 최대 길이(공백 포함). 피그마에 "개발과 협의"로 남아 있던 값
    @Min(1)
    private int maxAnswerLength;

    @Valid
    private Ai ai = new Ai();

    @Getter
    @Setter
    public static class Ai {

        // 검토 전용 모델. 노션 7장 제안처럼 바꿀 때 yml만 수정한다
        @NotBlank
        private String model;

        // 분석은 같은 글에 비슷한 결과가 나오도록 낮게 둔다
        @DecimalMin("0.0")
        @DecimalMax("2.0")
        private double analysisTemperature;

        // 후속 질문은 사용자 답에 맞춰 조금 더 유연하게
        @DecimalMin("0.0")
        @DecimalMax("2.0")
        private double questionTemperature;

        // 다듬기는 답에 없는 내용을 보태지 않도록 가장 낮게
        @DecimalMin("0.0")
        @DecimalMax("2.0")
        private double refineTemperature;
    }
}
