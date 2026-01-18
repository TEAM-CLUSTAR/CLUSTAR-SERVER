package org.project.domain.ai.service;

import org.project.domain.ai.dto.response.AiEvaluationResult;
import org.project.domain.ai.dto.response.MemoAiResponse;

public interface AiEvaluationService {

    AiEvaluationResult evaluate(
            String userPrompt,
            MemoAiResponse response
    );
}
