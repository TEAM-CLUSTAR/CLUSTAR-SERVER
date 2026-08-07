package org.project.domain.memo.repository;

import java.util.List;
import java.util.Map;

public interface MemoImageRepositoryCustom {

    Map<Long, String> findRepresentativeImageS3Keys(List<Long> memoIds);

    Map<Long, Long> countImagesByMemoId(List<Long> memoIds);
}
