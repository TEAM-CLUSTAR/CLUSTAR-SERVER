package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.RagPromptConfigRequest;
import org.project.domain.ai.dto.response.RagPromptConfigResponse;
import org.project.domain.ai.strategy.MemoAiOptions;

public interface PromptConfigService {

    RagPromptConfigResponse get(MemoAiOptions option);

    RagPromptConfigResponse upsert(MemoAiOptions option, RagPromptConfigRequest request);
}
