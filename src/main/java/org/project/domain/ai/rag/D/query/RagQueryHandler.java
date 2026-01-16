package org.project.domain.ai.rag.D.query;

import org.project.domain.ai.dto.request.MemoAiRequest;

public interface RagQueryHandler {

    RagQuery handle(
            Long userId,
            MemoAiRequest request
    );
}
