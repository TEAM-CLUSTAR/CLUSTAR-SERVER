package org.project.domain.memo.dto.response;

import org.project.domain.memo.entity.Memo;

public record MemoRecommendationItemResponse(
        Long memoId,
        String title
) {
    public static MemoRecommendationItemResponse from(Memo memo) {
        return new MemoRecommendationItemResponse(memo.getId(), memo.getTitle());
    }
}
