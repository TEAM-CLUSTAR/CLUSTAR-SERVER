package org.project.domain.memo.dto.response;

import java.util.List;

public record MemoStructureListResponse(
        List<MemoStructureResponse> memos
) {
    public static MemoStructureListResponse from(List<MemoStructureResponse> memos) {
        return new MemoStructureListResponse(memos);
    }
}
