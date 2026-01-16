package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.RagMemoCreateRequest;
import org.project.domain.ai.dto.response.RagMemoCreateResponse;

public interface MemoRagService {

    RagMemoCreateResponse createRagMemo(Long userId, RagMemoCreateRequest request);
}
