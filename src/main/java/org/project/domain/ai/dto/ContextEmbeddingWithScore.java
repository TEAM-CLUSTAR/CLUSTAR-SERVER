package org.project.domain.ai.dto;

public interface ContextEmbeddingWithScore {

    Long getId();
    Long getMemoId();
    String getContextType();
    Long getContextId();
    Integer getChunkIndex();
    String getChunkedContent();
    String getContent();     // 실제 텍스트
    String getModel();

    Double getSimilarity();
}
