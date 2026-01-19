package org.project.domain.memo.dto.response;

import org.project.domain.label.entity.Label;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;

import java.time.LocalDateTime;
import java.util.List;

public record MemoListDashboardResponse(
        List<MemoDashboardResponse> memos
) {

    public static MemoListDashboardResponse from(List<MemoDashboardResponse> memos) {
        return new MemoListDashboardResponse(memos);
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
                    content,
                    representativeImageUrl,
                    imageCount,
                    fileCount,
                    memo.getIsPinned(),
                    memo.getIsAiGenerated(),
                    memo.getCreatedAt(),
                    memo.getMemoLabels().stream()
                            .map(MemoLabel::getLabel)
                            .map(LabelResponse::from)
                            .toList()
            );
        }

        public static MemoDashboardResponse of(
                Memo memo,
                String representativeImageUrl,
                int imageCount,
                int fileCount
        ) {
            return of(memo, memo.getContent(), representativeImageUrl, imageCount, fileCount);
        }
    }

    /**
     * 라벨 응답
     */
    public record LabelResponse(
            Long labelId,
            String name
    ) {

        public static LabelResponse from(Label label) {
            return new LabelResponse(
                    label.getId(),
                    label.getName()
            );
        }
    }
}
