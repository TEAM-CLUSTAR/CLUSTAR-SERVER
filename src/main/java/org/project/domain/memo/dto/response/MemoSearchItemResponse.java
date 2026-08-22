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
        // 마지막 열람 시각(카드 날짜 표기용). 한 번도 열람하지 않은 메모는 null 이때, 어떻게 응답을 내려줄 지는 추후 결정 예정
        @Schema(nullable = true)
        LocalDateTime lastViewedAt,
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
                memo.getLastViewedAt(),
                searchType
        );
    }
}
