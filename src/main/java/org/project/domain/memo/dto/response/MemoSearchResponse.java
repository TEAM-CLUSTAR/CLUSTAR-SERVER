package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"results", "message"})
public record MemoSearchResponse(
        List<MemoSearchItemResponse> results,
        @Schema(nullable = true)
        String message
) {
    public static MemoSearchResponse of(List<MemoSearchItemResponse> results, String message) {
        return new MemoSearchResponse(results, message);
    }
}
