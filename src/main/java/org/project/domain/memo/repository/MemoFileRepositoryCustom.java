package org.project.domain.memo.repository;

import java.util.List;
import java.util.Map;

public interface MemoFileRepositoryCustom {

    Map<Long, Long> countFilesByMemoId(List<Long> memoIds);
}
