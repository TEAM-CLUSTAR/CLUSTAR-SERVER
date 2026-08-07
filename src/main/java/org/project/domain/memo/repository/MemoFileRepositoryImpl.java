package org.project.domain.memo.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.entity.QMemoFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MemoFileRepositoryImpl implements MemoFileRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 메모별 파일 개수만 집계
    @Override
    public Map<Long, Long> countFilesByMemoId(List<Long> memoIds) {
        QMemoFile memoFile = QMemoFile.memoFile;

        List<Tuple> results = queryFactory
                .select(memoFile.memo.id, memoFile.count())
                .from(memoFile)
                .where(memoFile.memo.id.in(memoIds))
                .groupBy(memoFile.memo.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(memoFile.memo.id),
                        t -> t.get(memoFile.count())
                ));
    }
}
