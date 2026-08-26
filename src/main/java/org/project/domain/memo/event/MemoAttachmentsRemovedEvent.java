package org.project.domain.memo.event;

import java.util.List;

// 메모 수정으로 첨부가 제거되면 발행 → 제거된 첨부의 벡터·S3를 커밋 후 정리
public record MemoAttachmentsRemovedEvent(
        Long memoId,
        List<Long> removedImageIds,
        List<Long> removedFileIds,
        List<String> removedImageKeys,
        List<String> removedFileKeys
) {
}
