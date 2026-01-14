package org.project.domain.memo.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class MemoDeletedEvent {
    private final Long memoId;
    private final List<String> imageKeys;
    private final List<String> fileKeys;
}
