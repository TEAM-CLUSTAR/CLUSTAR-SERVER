package org.project.domain.ai.repository;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.entity.ContextType;
import org.project.domain.ai.dto.response.RagContextChunkResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ContextEmbeddingSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * pgvector 유사도 검색을 위한 native query.
     * - vector literal은 "[1,2,3]" 형태로 전달해야 한다.
     */
    public List<RagContextChunkResponse> searchTopK(
            Long userId,
            List<Long> memoIds,
            String vectorLiteral,
            int topK
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT context_type, context_id, chunk_index, chunk_text
                FROM context_embedding
                WHERE user_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (memoIds != null && !memoIds.isEmpty()) {
            String placeholders = memoIds.stream()
                    .map(t -> "?")
                    .collect(Collectors.joining(", "));
            sql.append(" AND memo_id IN (").append(placeholders).append(")");
            params.addAll(memoIds);
        }

        sql.append(" ORDER BY embedding <-> ?::vector LIMIT ?");
        params.add(vectorLiteral);
        params.add(topK);

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                this::mapRow
        );
    }

    private RagContextChunkResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RagContextChunkResponse(
                ContextType.valueOf(rs.getString("context_type")),
                rs.getLong("context_id"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text")
        );
    }
}
