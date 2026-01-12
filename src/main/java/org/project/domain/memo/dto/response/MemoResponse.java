package org.project.domain.memo.dto.response;

import org.project.domain.memo.entity.Memo;

import java.time.LocalDateTime;

public record MemoResponse (
        Long memoId,

        String title,

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
