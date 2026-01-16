package org.project.domain.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.project.global.entity.BaseEntity;

@Entity
@Table(
        name = "context_embedding",
        indexes = {
                @Index(
                        name = "idx_context_embedding_context",
                        columnList = "context_type, context_id"
                ),
                @Index(
                        name = "idx_context_embedding_chunk",
                        columnList = "context_type, context_id, chunk_index"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ContextEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MEMO / MEMO_IMAGE / MEMO_FILE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 20)
    private ContextType contextType;

    /**
     * memo.id / memo_image.id / memo_file.id
     */
    @Column(name = "context_id", nullable = false)
    private Long contextId;

    @Column(name = "memo_id", nullable = false)
    private Long memoId;

    /**
     * 청킹 순서 (0부터 시작)
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * RAG용 실제 chunk 본문 (임베딩과 1:1)
     */
    @Column(name = "chunked_content", columnDefinition = "TEXT", nullable = false)
    private String chunkedContent;

    /**
     * Vector (pgvector)
     * Gemini text-embedding-004 = 768 dimensions
     */
    @Column(name = "embedding", nullable = false)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private float[] embedding;

    /**
     * embedding model name
     * ex) text-embedding-004
     */
    @Column(name = "model", nullable = false, length = 100)
    private String model;
}
