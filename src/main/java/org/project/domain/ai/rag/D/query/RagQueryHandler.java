package org.project.domain.ai.rag.D.query;

import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.rag.D.query.dto.RagQuery;

public interface RagQueryHandler {

    RagQuery handle(
            Long userId,
            MemoAiRequest request
    );
}
