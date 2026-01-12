package org.project.domain.memo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.project.domain.memo.entity.Memo;

import java.time.LocalDateTime;

public record MemoResponse (
        @Schema(description = "메모 ID", example = "1")
        Long memoId,

        @Schema(description = "메모 제목", example = "세미나 정리")
        String title,

        @Schema(description = "생성 시각", example = "2026-01-12")
        LocalDateTime createdAt
) {

    public static MemoResponse from(Memo memo){
        return new MemoResponse(
                memo.getId(),
                memo.getTitle(),
                memo.getCreatedAt()
        );
    }
}
