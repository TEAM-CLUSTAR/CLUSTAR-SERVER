package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.project.global.util.MarkdownUtil;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

@Schema(requiredProperties = {"memoId", "title", "content", "tagList", "createdAt", "searchType"})
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
