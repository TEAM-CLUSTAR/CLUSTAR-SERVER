package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"results", "message"})
public record MemoRecommendationResponse(
        List<MemoRecommendationItemResponse> results,
        @Schema(nullable = true)
        String message
) {
    public static MemoRecommendationResponse of(List<MemoRecommendationItemResponse> results) {
        if (results.isEmpty()) {
            return new MemoRecommendationResponse(List.of(), "선택한 메모와 관련된 메모를 찾지 못했어요.");
        }
        return new MemoRecommendationResponse(results, null);
    }

    public static MemoRecommendationResponse empty(String message) {
        return new MemoRecommendationResponse(List.of(), message);
    }
}
