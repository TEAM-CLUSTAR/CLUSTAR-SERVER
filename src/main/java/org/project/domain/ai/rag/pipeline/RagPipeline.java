package org.project.domain.ai.rag.pipeline;

import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;

public interface RagPipeline {

    MemoAiResponse run(
            Long userId,
            MemoAiRequest request
    );

    MemoAiResponse runForPlan(
            Long userId,
            MemoAiRequest request,
            String planPrompt
    );
}
