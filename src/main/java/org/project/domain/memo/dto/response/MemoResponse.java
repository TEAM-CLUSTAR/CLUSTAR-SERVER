package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;

import java.time.LocalDateTime;

@Schema(requiredProperties = {"memoId", "title", "createdAt", "updatedAt"})
public record MemoResponse (
        @Schema(description = "메모 ID", example = "1")
        Long memoId,

        @Schema(description = "메모 제목", example = "세미나 정리")
        String title,

        @Schema(description = "생성 시각", example = "2026-01-12T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "마지막 수정 시각", example = "2026-01-12T11:00:00")
        LocalDateTime updatedAt
) {

    public static MemoResponse from(Memo memo){
        return new MemoResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
