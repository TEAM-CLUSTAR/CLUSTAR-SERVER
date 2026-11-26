package org.project.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.review.entity.ReviewMessage;
import org.project.domain.review.entity.ReviewMessageRole;

import java.time.LocalDateTime;

@Schema(requiredProperties = {"messageId", "role", "content", "createdAt"})
public record ReviewMessageResponse(
        @Schema(description = "대화 ID")
        Long messageId,

        @Schema(description = "USER: 사용자의 답 / AI: AI 후속 질문")
        ReviewMessageRole role,

        @Schema(description = "내용")
        String content,

        @Schema(description = "작성 시각")
        LocalDateTime createdAt
) {
    public static ReviewMessageResponse from(ReviewMessage message) {
        return new ReviewMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
