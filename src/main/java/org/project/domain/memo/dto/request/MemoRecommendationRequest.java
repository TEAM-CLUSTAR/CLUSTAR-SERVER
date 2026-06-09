package org.project.domain.memo.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MemoRecommendationRequest(
        @NotEmpty(message = "선택된 메모가 없습니다.")
        List<Long> memoIds
) {
}
