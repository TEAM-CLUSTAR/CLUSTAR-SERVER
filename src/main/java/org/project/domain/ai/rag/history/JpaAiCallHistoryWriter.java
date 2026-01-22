package org.project.domain.ai.rag.history;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaAiCallHistoryWriter {

    private final AiCallHistoryRepository repository;

    /**
     * AI 호출 성공 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccess(
            RagPrompt prompt,
            String fullPrompt,
            String responseText,
            long latencyMs
    ) {
        repository.save(
                AiCallHistoryRecord.success(
                        prompt,
                        fullPrompt,
                        responseText,
                        latencyMs
                )
        );
    }

    /**
     * AI 호출 실패 기록
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(
            RagPrompt prompt,
            String fullPrompt,
            Exception e
    ) {
        repository.save(
                AiCallHistoryRecord.failure(
                        prompt,
                        fullPrompt,
                        e
                )
        );
    }
}
