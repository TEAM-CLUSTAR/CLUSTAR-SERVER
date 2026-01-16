package org.project.domain.ai.rag.D.query;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultRagQueryHandler implements RagQueryHandler {

    @Override
    public RagQuery handle(
            Long userId,
            MemoAiRequest request
    ) {
        return RagQuery.of(
                userId,
                request.option(),
                request.userPrompt(),
                request.memoIds()
        );
    }
}
