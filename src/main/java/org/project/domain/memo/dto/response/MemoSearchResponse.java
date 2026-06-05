package org.project.domain.memo.dto.response;

import java.util.List;

public record MemoSearchResponse(
        List<MemoSearchItemResponse> results
) {
    public static MemoSearchResponse from(List<MemoSearchItemResponse> results) {
        return new MemoSearchResponse(results);
    }
}
