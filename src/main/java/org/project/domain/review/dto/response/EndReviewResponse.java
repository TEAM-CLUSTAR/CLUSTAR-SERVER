package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"memoId", "currentVersion"})
public record EndReviewResponse(
        @Schema(description = "메모 ID")
        Long memoId,

        @Schema(description = "현재 문서 버전(방금 종료한 검토의 버전). 01 화면의 '문서 vN'에 쓴다")
        int currentVersion
) {
}
