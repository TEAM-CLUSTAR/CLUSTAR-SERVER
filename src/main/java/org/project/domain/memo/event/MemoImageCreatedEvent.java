package org.project.domain.memo.event;

import org.project.domain.memo.entity.MemoImage;

import java.util.List;

public record MemoImageCreatedEvent(
        Long userId,
        List<MemoImage> memoImages
) {
}
