package org.project.domain.memo.dto.response;

import java.util.List;

public record MemoRecommendationResponse(
        List<MemoRecommendationItemResponse> results,
        String message
) {
    public static MemoRecommendationResponse of(List<MemoRecommendationItemResponse> results) {
        if (results.isEmpty()) {
            return new MemoRecommendationResponse(List.of(), "선택한 메모와 관련된 메모를 찾지 못했어요.");
        }
        return new MemoRecommendationResponse(results, null);
    }
}
