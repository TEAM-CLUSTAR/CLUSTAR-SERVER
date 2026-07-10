package org.project.domain.memo.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final int FETCH_LIMIT = 9;

    public List<Long> findRecommendedMemoIds(Long userId, List<Long> memoIds, double similarityThreshold) {
        String ids = memoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // threshold는 SQL에서 걸지 않고 가장 가까운 후보 FETCH_LIMIT개를 전부 받아서 애플리케이션 레벨에서 거른다.
        // (SQL WHERE로 걸러버리면 컷된 후보는 아예 안 돌아와서, 로그로도 못 남기고
        //  나중에 threshold를 재조정할 때 "얼마나 아깝게 컷됐는지"를 알 수 없게 된다.)
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
                SELECT memo_id, 1 - min_distance AS similarity
                FROM scored
                ORDER BY min_distance ASC
                LIMIT %d
                """, ids, ids, FETCH_LIMIT);

        List<Object[]> rows = jdbcTemplate.query(sql,
                (rs, rowNum) -> new Object[]{rs.getLong("memo_id"), rs.getDouble("similarity")},
                userId, userId);

        List<Long> passedMemoIds = new ArrayList<>();
        for (Object[] row : rows) {
            Long candidateMemoId = (Long) row[0];
            double similarity = (double) row[1];
            boolean passed = similarity >= similarityThreshold;

            // 향후 threshold 재조정을 위해 통과/탈락 여부와 무관하게 전부 로그로 남긴다 (임시값, 실사용 데이터로 재산정 예정)
            log.info("[Recommendation][Gate2] userId={} selected={} candidateMemoId={} similarity={} threshold={} passed={}",
                    userId, memoIds, candidateMemoId, similarity, similarityThreshold, passed);

            if (passed) {
                passedMemoIds.add(candidateMemoId);
            }
        }

        return passedMemoIds;
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

        Double cohesion = jdbcTemplate.queryForObject(sql, Double.class, userId);
        log.info("[Recommendation][Gate1] userId={} selected={} cohesion={}", userId, memoIds, cohesion);
        return cohesion;
    }
}
