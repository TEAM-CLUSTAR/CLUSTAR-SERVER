package org.project.domain.memo.event;

import java.util.List;

// 휴지통 메모가 영구 삭제됨. 커밋 후 S3 첨부를 정리한다(벡터는 영구 삭제 트랜잭션에서 함께 지움).
// (휴지통 이동만으로는 발행하지 않는다 — 복원에 대비해 첨부·벡터를 남겨 둠)
public record MemoPurgedEvent(List<PurgedMemo> memos) {

    public record PurgedMemo(Long memoId, List<String> imageKeys, List<String> fileKeys) {
    }

    public List<Long> memoIds() {
        return memos.stream().map(PurgedMemo::memoId).toList();
    }
}
