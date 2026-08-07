package org.project.domain.memo.repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.domain.memo.entity.QMemoImage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MemoImageRepositoryImpl implements MemoImageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // 메모별 대표 이미지(우선순위 최소값)의 s3Key만 조회 — image_bytes/name/extension 등 안 쓰는 컬럼은 안 가져옴
    @Override
    public Map<Long, String> findRepresentativeImageS3Keys(List<Long> memoIds) {
        QMemoImage memoImage = QMemoImage.memoImage;
        QMemoImage subImage = new QMemoImage("subImage");

        List<Tuple> results = queryFactory
                .select(memoImage.memo.id, memoImage.imageS3Key)
                .from(memoImage)
                .where(
                        memoImage.memo.id.in(memoIds),
                        memoImage.imagePriority.eq(
                                JPAExpressions.select(subImage.imagePriority.min())
                                        .from(subImage)
                                        .where(subImage.memo.id.eq(memoImage.memo.id))
                        )
                )
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(memoImage.memo.id),
                        t -> t.get(memoImage.imageS3Key),
                        (a, b) -> a
                ));
    }

    // 메모별 이미지 개수만 집계 (행 자체를 안 끌고 옴)
    @Override
    public Map<Long, Long> countImagesByMemoId(List<Long> memoIds) {
        QMemoImage memoImage = QMemoImage.memoImage;

        List<Tuple> results = queryFactory
                .select(memoImage.memo.id, memoImage.count())
                .from(memoImage)
                .where(memoImage.memo.id.in(memoIds))
                .groupBy(memoImage.memo.id)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        t -> t.get(memoImage.memo.id),
                        t -> t.get(memoImage.count())
                ));
    }
}
