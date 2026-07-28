package org.project.domain.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "embedding_failure")
public class EmbeddingFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "embedding_failure_id")
    private Long id;

    @Column(name = "memo_id", nullable = false)
    private Long memoId;

    @Column(name = "embedding_type", nullable = false, length = 20) // "text" | "image" | "file" | "unknown"
    private String embeddingType;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    public static EmbeddingFailure create(Long memoId, String embeddingType, Exception e) {
        return EmbeddingFailure.builder()
                .memoId(memoId)
                .embeddingType(embeddingType)
                .failedAt(LocalDateTime.now())
                .errorMessage(safeMessage(e))
                .isResolved(false)
                .build();
    }

    public void resolve() {
        this.isResolved = true;
    }

    private static String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return null;
        }
        return e.getMessage().length() > 2000
                ? e.getMessage().substring(0, 2000)
                : e.getMessage();
    }
}
