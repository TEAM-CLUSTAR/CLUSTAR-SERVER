package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.entity.ContextType;
import org.project.domain.ai.repository.ContextEmbeddingRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextEmbeddingServiceImpl implements ContextEmbeddingService{

    private static final String MODEL_NAME = "text-embedding-004";
    private static final int PREVIEW_LENGTH = 300;

    private final EmbeddingModel embeddingModel;
    private final ContextEmbeddingRepository embeddingRepository;

    /**
     * 단일 텍스트 → embedding 벡터 생성
     */
    public List<Double> generateEmbedding(String text) {
        EmbeddingResponse response = embeddingModel.embed(text);
        return response.getResult().getOutput();
    }

    /**
     * 메모 embedding 저장
     * (memo 생성 / 수정 시 호출)
     */
    @Transactional
    public void saveMemoEmbedding(Long memoId, String memoText) {

        // 수정 시 기존 embedding 제거
        embeddingRepository.deleteByContextTypeAndContextId(
                ContextType.MEMO,
                memoId
        );

        // 현재는 chunk 1개 (추후 청킹 확장 가능)
        List<Double> vector = generateEmbedding(memoText);

        ContextEmbedding embedding = ContextEmbedding.builder()
                .contextType(ContextType.MEMO)
                .contextId(memoId)
                .chunkIndex(0)
                .sourcePreview(preview(memoText))
                .embedding(toFloatArray(vector))
                .model(MODEL_NAME)
                .build();

        embeddingRepository.save(embedding);
    }

    private String preview(String text) {
        if (text == null) return null;
        return text.length() <= PREVIEW_LENGTH
                ? text
                : text.substring(0, PREVIEW_LENGTH);
    }

    private float[] toFloatArray(List<Double> vector) {
        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }
}
