package org.project.domain.memo.event;

import org.project.domain.memo.entity.MemoFile;

import java.util.List;

public record MemoFileCreatedEvent(
        List<MemoFile> memoFiles
) {
}
