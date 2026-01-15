package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.entity.ContextType;
import org.project.domain.ai.repository.ContextEmbeddingRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
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
        float[] vector = generateEmbedding(memoText);

        ContextEmbedding embedding = ContextEmbedding.builder()
                .contextType(ContextType.MEMO)
                .contextId(memoId)
                .chunkIndex(0)
                .sourcePreview(preview(memoText))
                .embedding(vector)
                .model(MODEL_NAME)
                .build();

        embeddingRepository.save(embedding);
    }

    /**
     * 이미지 설명 embedding 저장
     * (이미지 업로드 / 수정 시 호출)
     */
    @Transactional
    public void saveImageEmbedding(Long imageId, String imageDescription) {

        // 수정 시 기존 embedding 제거
        embeddingRepository.deleteByContextTypeAndContextId(
                ContextType.MEMO_IMAGE,
                imageId
        );

        // 현재는 chunk 1개 (추후 청킹 확장 가능)
        float[] vector = generateEmbedding(imageDescription);

        ContextEmbedding embedding = ContextEmbedding.builder()
                .contextType(ContextType.MEMO_IMAGE)
                .contextId(imageId)
                .chunkIndex(0)
                .sourcePreview(preview(imageDescription))
                .embedding(vector)
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
}
