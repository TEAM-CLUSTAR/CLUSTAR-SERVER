package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.project.global.util.MarkdownUtil;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 검색 모달 [입력 완료 전] 화면의 "최근 열람한 메모" 목록 응답.
 * 최근 열람순(lastViewedAt DESC)으로 정렬된 카드 리스트를 담는다.
 */
@Schema(requiredProperties = {"results"})
public record MemoRecentViewedResponse(
        List<Item> results
) {
    public static MemoRecentViewedResponse of(List<Item> results) {
        return new MemoRecentViewedResponse(results);
    }

    @Schema(requiredProperties = {"memoId", "title", "content", "tagList", "lastViewedAt"})
    public record Item(
            Long memoId,
            String title,
            String content,
            List<MemoListDashboardResponse.TagResponse> tagList,
            // 마지막 열람 시각(카드 날짜 표기용). 최근 열람 목록이므로 항상 값이 존재한다.
            LocalDateTime lastViewedAt
    ) {
        public static Item from(Memo memo) {
            return new Item(
                    memo.getId(),
                    memo.getTitle(),
                    MemoContentUtils.truncateForDashboard(MarkdownUtil.strip(memo.getContent())),
                    memo.getMemoTags().stream()
                            .map(MemoTag::getTag)
                            .map(MemoListDashboardResponse.TagResponse::from)
                            .toList(),
                    memo.getLastViewedAt()
            );
        }
    }
}
