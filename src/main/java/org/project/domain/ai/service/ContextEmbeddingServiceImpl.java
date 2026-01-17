package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.entity.ContextType;
import org.project.domain.ai.repository.ContextEmbeddingRepository;
import org.project.global.util.embedding.TextChunker;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextEmbeddingServiceImpl implements ContextEmbeddingService{

    private static final String MODEL_NAME = "text-embedding-004";
    private static final int PREVIEW_LENGTH = 100;

    private final EmbeddingModel embeddingModel;
    private final ContextEmbeddingRepository embeddingRepository;

    private final TextChunker textChunker;

    /**
     * 단일 텍스트 → embedding 벡터 생성
     */
    @Override
    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * 메모 embedding 저장
     * (memo 생성 / 수정 시 호출)
     */
    @Override
    @Transactional
    public void saveMemoEmbedding(Long userId, Long memoId, String memoText) {

        // 수정 시 기존 embedding 제거
        embeddingRepository.deleteByContextTypeAndContextId(
                ContextType.MEMO,
                memoId
        );

        // 텍스트 -> 청크로 분할
        List<String> chunks = textChunker.chunk(memoText);

        // 각 청크를 임베딩으로 변환 + 저장
        int index = 0;
        for (String chunk : chunks) {

            float[] vector = generateEmbedding(chunk);

            // RAG 검색용으로 memoId/userId/chunkText를 함께 저장한다.
            ContextEmbedding embedding = ContextEmbedding.builder()
                    .contextType(ContextType.MEMO)
                    .contextId(memoId)
                    .memoId(memoId)
                    .userId(userId)
                    .chunkIndex(index++)
                    .chunkText(chunk)
                    .sourcePreview(preview(chunk))
                    .embedding(vector)
                    .model(MODEL_NAME)
                    .build();

            embeddingRepository.save(embedding);
        }
    }

    /**
     * 이미지 설명 embedding 저장
     * (이미지 업로드 / 수정 시 호출)
     */
    @Override
    @Transactional
    public void saveImageEmbedding(Long userId, Long memoId, Long imageId, String imageDescription) {

        // 수정 시 기존 embedding 제거
        embeddingRepository.deleteByContextTypeAndContextId(
                ContextType.MEMO_IMAGE,
                imageId
        );

        List<String> chunks = textChunker.chunk(imageDescription);

        int index = 0;
        for (String chunk : chunks) {

            float[] vector = generateEmbedding(chunk);

            // 이미지 설명도 메모 본문과 동일하게 검색 컨텍스트로 사용한다.
            ContextEmbedding embedding = ContextEmbedding.builder()
                    .contextType(ContextType.MEMO_IMAGE)
                    .contextId(imageId)
                    .memoId(memoId)
                    .userId(userId)
                    .chunkIndex(index++)
                    .chunkText(chunk)
                    .sourcePreview(preview(chunk))
                    .embedding(vector)
                    .model(MODEL_NAME)
                    .build();

            embeddingRepository.save(embedding);
        }
    }

    @Override
    @Transactional
    public void saveFileEmbedding(Long userId, Long memoId, Long fileId, String content) {

        // 수정 시 기존 embedding 제거
        embeddingRepository.deleteByContextTypeAndContextId(
                ContextType.MEMO_FILE,
                fileId
        );

        List<String> chunks = textChunker.chunk(content);

        int index = 0;
        for (String chunk : chunks) {

            float[] vector = generateEmbedding(chunk);

            // 파일 본문도 청크 단위로 저장해 RAG에서 재사용한다.
            ContextEmbedding embedding = ContextEmbedding.builder()
                    .contextType(ContextType.MEMO_FILE)
                    .contextId(fileId)
                    .memoId(memoId)
                    .userId(userId)
                    .chunkIndex(index++)
                    .chunkText(chunk)
                    .sourcePreview(preview(chunk))
                    .embedding(vector)
                    .model(MODEL_NAME)
                    .build();

            embeddingRepository.save(embedding);
        }
    }

    private String preview(String text) {
        if (text == null) return null;
        return text.length() <= PREVIEW_LENGTH
                ? text
                : text.substring(0, PREVIEW_LENGTH);
    }
}
