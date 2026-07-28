package org.project.domain.memo.dto.response;

import java.util.List;

public record MemoSearchResponse(
        List<MemoSearchItemResponse> results,
        String message
) {
    public static MemoSearchResponse of(List<MemoSearchItemResponse> results, String message) {
        return new MemoSearchResponse(results, message);
    }
}
