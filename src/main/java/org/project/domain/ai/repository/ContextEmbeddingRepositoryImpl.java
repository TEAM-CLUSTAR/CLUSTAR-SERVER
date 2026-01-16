package org.project.domain.ai.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.ContextEmbeddingWithScore;
import org.project.domain.ai.entity.QContextEmbedding;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContextEmbeddingRepositoryImpl
        implements ContextEmbeddingRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ContextEmbeddingWithScore> searchByMemoIds(
            List<Long> memoIds,
            float[] queryEmbedding,
            int limit
    ) {

        QContextEmbedding ce = QContextEmbedding.contextEmbedding;

        // cosine similarity: 1 - (embedding <=> query)
        NumberExpression<Double> similarity =
                Expressions.numberTemplate(
                        Double.class,
                        "1 - ({0} <=> {1})",
                        ce.embedding,
                        Expressions.constant(queryEmbedding)
                );

        return queryFactory
                .select(
                        Projections.constructor(
                                ContextEmbeddingWithScore.class,
                                ce,
                                similarity
                        )
                )
                .from(ce)
                .where(
                        ce.memoId.in(memoIds)
                )
                .orderBy(similarity.desc())
                .limit(limit)
                .fetch();
    }
}
