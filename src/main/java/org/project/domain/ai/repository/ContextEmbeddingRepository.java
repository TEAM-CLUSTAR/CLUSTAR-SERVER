package org.project.domain.ai.repository;

import org.project.domain.ai.dto.ContextEmbeddingWithScore;
import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.entity.ContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContextEmbeddingRepository
        extends JpaRepository<ContextEmbedding, Long> {

    List<ContextEmbedding> findByContextTypeAndContextId(
            ContextType contextType,
            Long contextId
    );

    void deleteByContextTypeAndContextId(
            ContextType contextType,
            Long contextId
    );

    @Query(
            value = """
                    SELECT
                        ce.id               AS id,
                        ce.memo_id          AS memoId,
                        ce.context_type     AS contextType,
                        ce.context_id       AS contextId,
                        ce.chunk_index      AS chunkIndex,
                        ce.source_preview   AS sourcePreview,
                        ce.model            AS model,
                        (1 - (ce.embedding <=> CAST(:queryEmbedding AS vector))) AS similarity
                    FROM context_embedding ce
                    WHERE ce.memo_id IN (:memoIds)
                    ORDER BY ce.embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ContextEmbeddingWithScore> searchByMemoIds(
            @Param("memoIds") List<Long> memoIds,
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit
    );

}
