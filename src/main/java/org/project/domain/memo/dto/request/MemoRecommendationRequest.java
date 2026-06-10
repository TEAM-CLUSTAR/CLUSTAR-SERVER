package org.project.domain.memo.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MemoRecommendationRequest(
        @NotEmpty(message = "선택된 메모가 없습니다.")
        List<@NotNull(message = "memoId는 null일 수 없습니다.") @Positive(message = "memoId는 1 이상이어야 합니다.") Long> memoIds
) {
}
