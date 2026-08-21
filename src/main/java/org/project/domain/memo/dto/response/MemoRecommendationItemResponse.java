package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;

@Schema(requiredProperties = {"memoId", "title"})
public record MemoRecommendationItemResponse(
        Long memoId,
        String title
) {
    public static MemoRecommendationItemResponse from(Memo memo) {
        return new MemoRecommendationItemResponse(memo.getId(), memo.getTitle());
    }
}
