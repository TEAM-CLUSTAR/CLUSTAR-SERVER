package org.project.domain.memo.dto.response;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.project.global.util.MarkdownUtil;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

public record MemoSearchItemResponse(
        Long memoId,
        String title,
        String content,
        List<MemoListDashboardResponse.TagResponse> tagList,
        LocalDateTime createdAt,
        SearchType searchType
) {
    public static MemoSearchItemResponse from(Memo memo, SearchType searchType) {
        return new MemoSearchItemResponse(
                memo.getId(),
                memo.getTitle(),
                MemoContentUtils.truncateForDashboard(MarkdownUtil.strip(memo.getContent())),
                memo.getMemoTags().stream()
                        .map(MemoTag::getTag)
                        .map(MemoListDashboardResponse.TagResponse::from)
                        .toList(),
                memo.getCreatedAt(),
                searchType
        );
    }
}
