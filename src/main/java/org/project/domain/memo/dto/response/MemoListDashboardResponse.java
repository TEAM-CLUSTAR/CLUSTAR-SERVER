package org.project.domain.memo.dto.response;

import org.project.domain.label.entity.Label;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;
import org.project.global.util.MemoContentUtils;

import java.time.LocalDateTime;
import java.util.List;

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
    public record MemoDashboardResponse(
            Long memoId,
            String title,
            String content,

            // 대표 이미지 (priority 가장 낮은 1개, presigned URL)
            String representativeImageUrl,

            // 이미지 / 파일 개수
            int imageCount,
            int fileCount,

            Boolean isPinned,
            Boolean isAiGenerated,
            Boolean isNew,
            LocalDateTime createdAt,

            List<LabelResponse> labelList
    ) {

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
                    memo.getMemoLabels().stream()
                            .map(MemoLabel::getLabel)
                            .map(LabelResponse::from)
                            .toList()
            );
        }
    }

    /**
     * 라벨 응답
     */
    public record LabelResponse(
            Long labelId,
            String name,
            String colorHex
    ) {

        public static LabelResponse from(Label label) {
            return new LabelResponse(
                    label.getId(),
                    label.getName(),
                    label.getColorHex()
            );
        }
    }
}
