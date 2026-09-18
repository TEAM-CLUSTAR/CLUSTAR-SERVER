package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.tag.entity.Tag;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

@Schema(requiredProperties = {"totalCount", "memos"})
public record MemoListDashboardResponse(
        long totalCount,
        List<MemoDashboardResponse> memos
) {

    public static MemoListDashboardResponse from(long totalCount, List<MemoDashboardResponse> memos
                                                 ) {
        return new MemoListDashboardResponse(totalCount, memos);
    }

    /**
     * 대시보드용 메모 응답
     */
    @Schema(requiredProperties = {"memoId", "title", "content", "representativeImageUrl", "imageCount", "fileCount", "isPinned", "isAiGenerated", "isNew", "createdAt", "updatedAt", "tagList"})
    public record MemoDashboardResponse(
            Long memoId,
            String title,
            String content,

            // 대표 이미지 (priority 가장 낮은 1개, presigned URL)
            @Schema(nullable = true)
            String representativeImageUrl,

            // 이미지 / 파일 개수
            int imageCount,
            int fileCount,

            Boolean isPinned,
            Boolean isAiGenerated,
            Boolean isNew,
            LocalDateTime createdAt,
            @Schema(description = "메모 마지막 수정 시각")
            LocalDateTime updatedAt,
            @Schema(nullable = true, description = "마지막 열람 시각. 다음 페이지 조회 시 cursorLastViewedAt에 전달합니다.")
            LocalDateTime lastViewedAt,

            List<TagResponse> tagList
    ) {

        public MemoDashboardResponse(
                Long memoId, String title, String content, String representativeImageUrl,
                int imageCount, int fileCount, Boolean isPinned, Boolean isAiGenerated,
                Boolean isNew, LocalDateTime createdAt, List<TagResponse> tagList
        ) {
            this(memoId, title, content, representativeImageUrl, imageCount, fileCount,
                    isPinned, isAiGenerated, isNew, createdAt, createdAt, null, tagList);
        }

        public MemoDashboardResponse(
                Long memoId, String title, String content, String representativeImageUrl,
                int imageCount, int fileCount, Boolean isPinned, Boolean isAiGenerated,
                Boolean isNew, LocalDateTime createdAt, LocalDateTime lastViewedAt,
                List<TagResponse> tagList
        ) {
            this(memoId, title, content, representativeImageUrl, imageCount, fileCount,
                    isPinned, isAiGenerated, isNew, createdAt, createdAt, lastViewedAt, tagList);
        }

        /**
         * 엔티티 → DTO 변환
         * (presigned URL, count 값은 Service에서 계산 후 주입)
         */
        public static MemoDashboardResponse of(
                Memo memo,
                String content,
                String representativeImageUrl,
                int imageCount,
                int fileCount
        ) {
            return new MemoDashboardResponse(
                    memo.getId(),
                    memo.getTitle(),
                    MemoContentUtils.truncateForDashboard(content),
                    representativeImageUrl,
                    imageCount,
                    fileCount,
                    memo.getIsPinned(),
                    memo.getIsAiGenerated(),
                    memo.getIsNew(),
                    memo.getCreatedAt(),
                    memo.getUpdatedAt(),
                    memo.getLastViewedAt(),
                    memo.getMemoTags().stream()
                            .map(MemoTag::getTag)
                            .map(TagResponse::from)
                            .toList()
            );
        }
    }

    /**
     * 태그 응답
     */
    @Schema(requiredProperties = {"tagId", "name", "color"})
    public record TagResponse(
            Long tagId,
            String name,
            String color
    ) {

        public static TagResponse from(Tag tag) {
            return new TagResponse(
                    tag.getId(),
                    tag.getName(),
                    tag.getColor()
            );
        }
    }
}
