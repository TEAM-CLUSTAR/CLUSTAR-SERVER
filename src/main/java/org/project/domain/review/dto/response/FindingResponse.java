package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.review.entity.FindingCategory;
import org.project.domain.review.entity.FindingStatus;
import org.project.domain.review.entity.ReviewFinding;

import java.util.List;

@Schema(requiredProperties = {"findingId", "number", "category", "anchorText", "title",
        "reason", "evidence", "question", "status", "messages"})
public record FindingResponse(
        @Schema(description = "빈틈 카드 ID")
        Long findingId,

        @Schema(description = "본문 마커와 카드를 잇는 번호(1부터, 본문 순서)")
        int number,

        @Schema(description = "유형. CRITERIA: 판단 기준 / STAKEHOLDER: 빠진 사람 / EDGE_CASE: 예외 상황")
        FindingCategory category,

        @Schema(description = "위치(섹션 제목). 예: 2. 타깃")
        String sectionLabel,

        @Schema(description = "AI가 짚은 본문 문장 원문. 이 문장을 본문에서 찾아 밑줄과 번호를 표시한다")
        String anchorText,

        @Schema(description = "카드 제목")
        String title,

        @Schema(description = "짚은 곳")
        String reason,

        @Schema(description = "왜 빈틈으로 봤나요(근거)")
        String evidence,

        @Schema(description = "AI 첫 질문. 카드를 열면 바로 보여준다")
        String question,

        @Schema(description = "OPEN: 아직 답 없음 / ANSWERING: 답하는 중 / APPLIED: 반영됨")
        FindingStatus status,

        @Schema(description = "대화(사용자 답, AI 후속 질문). 오래된 순")
        List<ReviewMessageResponse> messages,

        @Schema(description = "본문에 넣은 문장. 반영 전에는 null")
        String appliedText
) {
    public static FindingResponse of(ReviewFinding finding, List<ReviewMessageResponse> messages) {
        return new FindingResponse(
                finding.getId(),
                finding.getNumber(),
                finding.getCategory(),
                finding.getSectionLabel(),
                finding.getAnchorText(),
                finding.getTitle(),
                finding.getReason(),
                finding.getEvidence(),
                finding.getQuestion(),
                finding.getStatus(),
                messages,
                finding.getAppliedText()
        );
    }
}
