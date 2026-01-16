package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.service.ContextEmbeddingService;
import org.project.domain.ai.service.MemoMediaAiService;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.event.MemoFileCreatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemoFileEmbeddingListener {

    private final MemoMediaAiService memoMediaAiService;
    private final ContextEmbeddingService contextEmbeddingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoFileCreatedEvent event) {

        for (MemoFile file : event.memoFiles()) {

            String content =
                    memoMediaAiService.extractText(
                            file.getFileS3Key()
                    );

            contextEmbeddingService.saveFileEmbedding(
                    file.getId(),
                    content
            );
        }
    }
}
