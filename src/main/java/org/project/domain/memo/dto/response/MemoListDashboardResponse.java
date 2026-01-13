package org.project.domain.memo.dto.response;

import org.project.domain.label.entity.Label;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;

import java.time.LocalDateTime;
import java.util.List;

public record MemoListDashboardResponse(
        List<MemoDashboardResponse> memos
) {

    public static MemoListDashboardResponse from(List<Memo> memos) {
        return new MemoListDashboardResponse(
                memos.stream()
                        .map(MemoDashboardResponse::from)
                        .toList()
        );
    }

    public record MemoDashboardResponse(
            Long memoId,
            String title,
            String content,
            Boolean isPinned,
            Boolean isAiGenerated,
            LocalDateTime createdAt,
            List<LabelResponse> labels
    ) {

        public static MemoDashboardResponse from(Memo memo) {
            return new MemoDashboardResponse(
                    memo.getId(),
                    memo.getTitle(),
                    memo.getContent(),
                    memo.getIsPinned(),
                    memo.getIsAiGenerated(),
                    memo.getCreatedAt(),
                    memo.getMemoLabels().stream()
                            .map(MemoLabel::getLabel)
                            .map(LabelResponse::from)
                            .toList()
            );
        }
    }

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
