package org.project.domain.ai.repository;

import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.entity.ContextType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContextEmbeddingRepository extends JpaRepository<ContextEmbedding, Long>, ContextEmbeddingRepositoryCustom {

    List<ContextEmbedding> findByContextTypeAndContextId(
            ContextType contextType,
            Long contextId
    );

    void deleteByContextTypeAndContextId(
            ContextType contextType,
            Long contextId
    );
}
