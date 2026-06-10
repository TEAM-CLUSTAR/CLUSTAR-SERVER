package org.project.domain.memo.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int FETCH_LIMIT = 9;

    public List<Long> findRecommendedMemoIds(Long userId, List<Long> memoIds) {
        String ids = memoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String sql = String.format("""
                WITH avg_vec AS (
                    SELECT AVG(embedding) AS vec
                    FROM vector_store
                    WHERE CAST(metadata->>'memoId' AS BIGINT) IN (%s)
                      AND CAST(metadata->>'userId' AS BIGINT) = ?
                    HAVING COUNT(*) > 0
                ), scored AS (
                    SELECT CAST(vs.metadata->>'memoId' AS BIGINT) AS memo_id,
                           MIN(vs.embedding <=> avg_vec.vec) AS min_distance
                    FROM vector_store vs
                    JOIN avg_vec ON TRUE
                    WHERE CAST(vs.metadata->>'userId' AS BIGINT) = ?
                      AND CAST(vs.metadata->>'memoId' AS BIGINT) NOT IN (%s)
                    GROUP BY CAST(vs.metadata->>'memoId' AS BIGINT)
                )
                SELECT memo_id
                FROM scored
                WHERE 1 - min_distance >= %s
                ORDER BY min_distance ASC
                LIMIT %d
                """, ids, ids, SIMILARITY_THRESHOLD, FETCH_LIMIT);

        return jdbcTemplate.queryForList(sql, Long.class, userId, userId);
    }
}
