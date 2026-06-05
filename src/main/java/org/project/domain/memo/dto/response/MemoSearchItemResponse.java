package org.project.domain.memo.dto.response;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;
import org.project.global.util.MarkdownUtil;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

public record MemoSearchItemResponse(
        Long memoId,
        String title,
        String content,
        List<MemoListDashboardResponse.LabelResponse> labelList,
        LocalDateTime createdAt,
        String searchType
) {
    public static MemoSearchItemResponse from(Memo memo, String searchType) {
        return new MemoSearchItemResponse(
                memo.getId(),
                memo.getTitle(),
                MemoContentUtils.truncateForDashboard(MarkdownUtil.strip(memo.getContent())),
                memo.getMemoLabels().stream()
                        .map(MemoLabel::getLabel)
                        .map(MemoListDashboardResponse.LabelResponse::from)
                        .toList(),
                memo.getCreatedAt(),
                searchType
        );
    }
}
