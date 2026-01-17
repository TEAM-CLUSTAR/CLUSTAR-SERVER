package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.AiPromptConfigRequest;
import org.project.domain.ai.dto.response.AiPromptResponse;
import org.project.domain.ai.strategy.MemoAiOptions;

public interface AiPromptConfigService {

    AiPromptResponse get(MemoAiOptions option);

    AiPromptResponse upsert(MemoAiOptions option, AiPromptConfigRequest request);
}
