package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.AiMemoCreateRequest;
import org.project.domain.ai.dto.response.AiMemoCreateResponse;

public interface AiMemoService {

    AiMemoCreateResponse createAiMemo(Long userId, AiMemoCreateRequest request);
}
