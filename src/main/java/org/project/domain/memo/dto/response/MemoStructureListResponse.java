package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MemoStructureListResponse(
        @Schema(description = "구조화뷰 메모 목록")
        List<MemoStructureResponse> memos
) {
    public static MemoStructureListResponse from(List<MemoStructureResponse> memos) {
        return new MemoStructureListResponse(memos);
    }
}
