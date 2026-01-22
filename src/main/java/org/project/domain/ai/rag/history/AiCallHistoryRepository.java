package org.project.domain.ai.rag.history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCallHistoryRepository extends JpaRepository<AiCallHistoryRecord, Long> {
}
