package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"memoId", "currentVersion"})
public record ActiveReviewResponse(
        @Schema(description = "메모 ID")
        Long memoId,

        @Schema(description = "현재 문서 버전. 한 번도 검토하지 않았으면 1(원본)")
        int currentVersion,

        @Schema(description = "진행 중인 검토. 없으면 null(검토 전 또는 종료 후 화면)", nullable = true)
        ReviewResponse review
) {
}
