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
 * 열람 이력이 하나도 없으면 서버가 최근 생성 메모(createdAt DESC)로 폴백해 채운다.
 */
@Schema(requiredProperties = {"source", "results"})
public record MemoRecentViewedResponse(
        @Schema(
                description = "목록 출처. RECENT_VIEWED=최근 열람 메모, RECENT_CREATED=열람 이력이 없어 최근 생성 메모로 폴백. "
        )
        RecentViewedSource source,
        List<Item> results
) {
    public static MemoRecentViewedResponse of(RecentViewedSource source, List<Item> results) {
        return new MemoRecentViewedResponse(source, results);
    }

    @Schema(requiredProperties = {"memoId", "title", "content", "tagList", "createdAt"})
    public record Item(
            Long memoId,
            String title,
            String content,
            List<MemoListDashboardResponse.TagResponse> tagList,
            // 마지막 열람 시각. 최근 열람 아이템이면 값이 있고, 최근 생성 폴백 아이템이면 null이다.
            // 값이 null이면 클라이언트는 폴백(최근 생성)으로 보고 createdAt으로 날짜를 표기한다.
            @Schema(
                    nullable = true,
                    description = "마지막 열람 시각. null이면 열람 이력이 없어 최근 생성 메모로 폴백된 아이템이라는 뜻이며, "
                            + "이 경우 클라이언트는 createdAt으로 날짜를 표기한다. null이 아니면 실제 최근 열람 메모다."
            )
            LocalDateTime lastViewedAt,
            // 생성 시각(카드 날짜 표기용 폴백). 항상 값이 존재한다.
            @Schema(description = "생성 시각. 항상 존재하며, 폴백(lastViewedAt=null) 아이템의 날짜 표기에 사용한다.")
            LocalDateTime createdAt
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
                    memo.getLastViewedAt(),
                    memo.getCreatedAt()
            );
        }
    }
}
