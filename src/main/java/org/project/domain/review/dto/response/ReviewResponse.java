package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.review.entity.Review;

import java.util.List;

@Schema(requiredProperties = {"reviewId", "version", "findings"})
public record ReviewResponse(
        @Schema(description = "검토 ID. 다시 검토하면 새 ID가 발급된다")
        Long reviewId,

        @Schema(description = "문서 버전(원본 v1, 검토할 때마다 +1)")
        int version,

        @Schema(description = "빈틈 카드 목록(번호순). 비어 있으면 짚을 곳을 찾지 못한 것")
        List<FindingResponse> findings
) {
    public static ReviewResponse of(Review review, List<FindingResponse> findings) {
        return new ReviewResponse(review.getId(), review.getVersion(), findings);
    }
}
