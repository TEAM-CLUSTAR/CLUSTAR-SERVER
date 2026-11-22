package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.review.entity.FindingStatus;

@Schema(requiredProperties = {"findingId", "status", "answer"})
public record AnswerResponse(
        @Schema(description = "빈틈 카드 ID")
        Long findingId,

        @Schema(description = "카드 상태. 답을 보내면 ANSWERING")
        FindingStatus status,

        @Schema(description = "방금 저장된 내 답")
        ReviewMessageResponse answer,

        @Schema(description = "AI 후속 질문. 첫 답일 때만 있고, 두 번째 답부터(또는 생성 실패 시)는 null → 프론트가 고정 안내 문구를 보여준다",
                nullable = true)
        ReviewMessageResponse aiMessage
) {
}
