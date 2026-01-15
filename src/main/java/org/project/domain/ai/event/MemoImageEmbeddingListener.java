package org.project.domain.ai.event;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.service.ContextEmbeddingService;
import org.project.domain.ai.service.MemoImageAiService;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.event.MemoImageCreatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemoImageEmbeddingListener {

    private final MemoImageAiService memoImageAiService;
    private final ContextEmbeddingService contextEmbeddingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemoImageCreatedEvent event) {

        for (MemoImage image : event.memoImages()) {
            String description =
                    memoImageAiService.generateImageDescription(
                            image.getImageS3Key()
                    );

            contextEmbeddingService.saveImageEmbedding(
                    image.getId(),
                    description
            );
        }
    }
}

