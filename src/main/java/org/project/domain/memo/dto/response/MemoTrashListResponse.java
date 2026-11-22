package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

@Schema(requiredProperties = {"totalCount", "memos"})
public record MemoTrashListResponse(
        @Schema(description = "휴지통에 있는 메모 총 개수")
        long totalCount,
        List<TrashMemoResponse> memos
) {

    public static MemoTrashListResponse from(long totalCount, List<TrashMemoResponse> memos) {
        return new MemoTrashListResponse(totalCount, memos);
    }

    @Schema(requiredProperties = {"memoId", "title", "content", "representativeImageUrl", "imageCount", "fileCount", "isAiGenerated", "createdAt", "updatedAt", "deletedAt", "tagList"})
    public record TrashMemoResponse(
            Long memoId,
            String title,
            String content,

            @Schema(nullable = true, description = "대표 이미지 (presigned URL)")
            String representativeImageUrl,

            int imageCount,
            int fileCount,
            Boolean isAiGenerated,
            LocalDateTime createdAt,
            @Schema(description = "메모 마지막 수정 시각")
            LocalDateTime updatedAt,
            @Schema(description = "휴지통으로 옮긴 시각. 다음 페이지 조회 시 cursorDeletedAt에 전달합니다.")
            LocalDateTime deletedAt,

            List<MemoListDashboardResponse.TagResponse> tagList
    ) {

        // content는 마크다운을 걷어낸 본문을 받아 카드용으로 자른다 (대시보드와 동일)
        public static TrashMemoResponse of(
                Memo memo,
                String content,
                String representativeImageUrl,
                int imageCount,
                int fileCount
        ) {
            return new TrashMemoResponse(
                    memo.getId(),
                    memo.getTitle(),
                    MemoContentUtils.truncateForDashboard(content),
                    representativeImageUrl,
                    imageCount,
                    fileCount,
                    memo.getIsAiGenerated(),
                    memo.getCreatedAt(),
                    memo.getUpdatedAt(),
                    memo.getDeletedAt(),
                    memo.getMemoTags().stream()
                            .map(MemoTag::getTag)
                            .map(MemoListDashboardResponse.TagResponse::from)
                            .toList()
            );
        }
    }
}
