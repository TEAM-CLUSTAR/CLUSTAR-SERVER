package org.project.domain.memo.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final int FETCH_LIMIT = 9;

    public List<Long> findRecommendedMemoIds(Long userId, List<Long> memoIds, double similarityThreshold) {
        String ids = memoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String sql = String.format(Locale.US, """
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
                """, ids, ids, similarityThreshold, FETCH_LIMIT);

        return jdbcTemplate.queryForList(sql, Long.class, userId, userId);
    }

    /**
     * 선택된 메모들이 서로 얼마나 응집돼 있는지(Gate 1)를 쌍별 코사인 유사도 평균으로 계산한다.
     * 메모가 1개뿐이면 응집도가 정의되지 않으므로 null을 반환한다(호출 측에서 Gate1을 건너뛰어야 함).
     */
    public Double computeSelectionCohesion(Long userId, List<Long> memoIds) {
        if (memoIds.size() < 2) {
            return null;
        }

        String ids = memoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String sql = String.format(Locale.US, """
                WITH selected AS (
                    SELECT CAST(metadata->>'memoId' AS BIGINT) AS memo_id, embedding
                    FROM vector_store
                    WHERE CAST(metadata->>'memoId' AS BIGINT) IN (%s)
                      AND CAST(metadata->>'userId' AS BIGINT) = ?
                ), pairs AS (
                    SELECT a.memo_id AS a_id, b.memo_id AS b_id,
                           MAX(1 - (a.embedding <=> b.embedding)) AS similarity
                    FROM selected a
                    JOIN selected b ON a.memo_id < b.memo_id
                    GROUP BY a.memo_id, b.memo_id
                )
                SELECT AVG(similarity) FROM pairs
                """, ids);

        return jdbcTemplate.queryForObject(sql, Double.class, userId);
    }
}
