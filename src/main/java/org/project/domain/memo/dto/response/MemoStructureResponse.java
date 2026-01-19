package org.project.domain.memo.dto.response;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MemoStructureResponse(
        @Schema(description = "메모 ID", example = "1")
        Long memoId,

        @Schema(description = "메모 제목", example = "집에 빨리 가는 법")
        String title,

        @Schema(description = "메모 내용", example = "발박수 치며 날아 간다.")
        String content,

        @Schema(description = "메모에 딸린 라벨 목록")
        List<MemoListDashboardResponse.LabelResponse> labelList
) {

    public static MemoStructureResponse from(
            Memo memo
    ) {
        return from(memo, memo.getContent());
    }

    public static MemoStructureResponse from(
            Memo memo,
            String content
    ) {
        return new MemoStructureResponse(
                memo.getId(),
                memo.getTitle(),
                content,
                memo.getMemoLabels().stream()
                        .map(MemoLabel::getLabel)
                        .map(MemoListDashboardResponse.LabelResponse::from)
                        .toList()
        );
    }
}
