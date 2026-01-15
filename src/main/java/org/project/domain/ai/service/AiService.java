package org.project.domain.ai.service;

import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;

import java.util.List;

public interface AiService {

    List<Double> generateEmbedding(String text);

    MemoAiResponse generateMemoAi(MemoAiRequest request);
}
