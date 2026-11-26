package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;
import org.project.domain.review.entity.FindingStatus;
import org.project.domain.review.entity.ReviewFinding;

import java.time.LocalDateTime;

@Schema(requiredProperties = {"memoId", "content", "updatedAt", "finding"})
public record ApplyResponse(
        @Schema(description = "메모 ID")
        Long memoId,

        @Schema(description = "반영 후 본문 전체. 프론트는 편집기 내용을 반드시 이 값으로 바꿔야 한다(자동 저장이 덮어쓰지 않도록)")
        String content,

        @Schema(description = "메모 수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "반영된 카드")
        AppliedFinding finding
) {
    public static ApplyResponse of(Memo memo, ReviewFinding finding) {
        return new ApplyResponse(
                memo.getId(),
                memo.getContent(),
                memo.getUpdatedAt(),
                new AppliedFinding(finding.getId(), finding.getStatus(), finding.getAppliedText())
        );
    }

    public record AppliedFinding(
            @Schema(description = "빈틈 카드 ID")
            Long findingId,

            @Schema(description = "APPLIED")
            FindingStatus status,

            @Schema(description = "본문에 넣은 문장. 검토 중 반영 블록 표시에 쓴다")
            String appliedText
    ) {
    }
}
