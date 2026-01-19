package org.project.domain.memo.dto.response;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;

import java.util.List;

public record MemoStructureResponse(
        Long memoId,

        String title,

        String content,

        List<MemoListDashboardResponse.LabelResponse> labelList
) {

    public static MemoStructureResponse from(
            Memo memo
    ) {
        return new MemoStructureResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getMemoLabels().stream()
                        .map(MemoLabel::getLabel)
                        .map(MemoListDashboardResponse.LabelResponse::from)
                        .toList()
        );
    }
}


