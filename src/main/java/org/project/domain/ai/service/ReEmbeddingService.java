package org.project.domain.ai.service;

import org.project.domain.ai.dto.response.EmbeddingFailureResponse;
import org.project.domain.ai.dto.response.ReEmbeddingResultResponse;

import java.util.List;

public interface ReEmbeddingService {

    void reEmbedAll();

    ReEmbeddingResultResponse reEmbedOne(Long memoId);

    List<EmbeddingFailureResponse> getUnresolvedFailures();
}
