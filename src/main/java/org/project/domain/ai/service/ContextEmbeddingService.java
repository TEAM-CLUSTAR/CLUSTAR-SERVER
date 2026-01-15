package org.project.domain.ai.service;

import java.util.List;

public interface ContextEmbeddingService {

    List<Double> generateEmbedding(String text);

    void saveMemoEmbedding(Long memoId, String memoText);
}
