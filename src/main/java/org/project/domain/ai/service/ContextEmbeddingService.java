package org.project.domain.ai.service;

import java.util.List;

public interface ContextEmbeddingService {

    float[] generateEmbedding(String text);

    void saveMemoEmbedding(Long memoId, String memoText);
}
