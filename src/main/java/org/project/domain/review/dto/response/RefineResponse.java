package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"findingId", "refinedText", "rawText"})
public record RefineResponse(
        @Schema(description = "빈틈 카드 ID")
        Long findingId,

        @Schema(description = "AI가 다듬은 문장. 질문마다 \"라벨: 문장\" 한 줄씩(줄바꿈 \\n). 미리보기 편집창에 넣는다",
                example = "선정 기준: 1~2학년이 기초 전공책을 제일 많이 사기 때문이다.\n그 외 학생: 3~4학년도 살 수 있지만, 홍보는 저학년 위주로 한다.")
        String refinedText,

        @Schema(description = "내 답 원문(보낸 순서대로 줄바꿈). '원문 비교'에 보여준다")
        String rawText
) {
}
