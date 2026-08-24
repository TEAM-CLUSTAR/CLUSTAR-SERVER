package org.project.domain.memo.event;

import java.util.List;

/**
 * 메모 수정으로 이미지/파일 첨부가 제거되었을 때 발행.
 * 제거된 첨부의 벡터(imageId/fileId 단위)와 S3 객체를 커밋 후 정리한다.
 */
public record MemoAttachmentsRemovedEvent(
        Long memoId,
        List<Long> removedImageIds,
        List<Long> removedFileIds,
        List<String> removedImageKeys,
        List<String> removedFileKeys
) {
}
