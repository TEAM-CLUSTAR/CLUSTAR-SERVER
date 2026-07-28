package org.project.domain.ai.repository;

import org.project.domain.ai.entity.EmbeddingFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbeddingFailureRepository extends JpaRepository<EmbeddingFailure, Long> {

    List<EmbeddingFailure> findByIsResolvedFalse();

    List<EmbeddingFailure> findByMemoIdAndEmbeddingTypeAndIsResolvedFalse(Long memoId, String embeddingType);
}
