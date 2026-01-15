package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new AiException(AiErrorCode.EMPTY_EMBEDDING_TEXT);
        }

        float[] embedding = embeddingModel.embed(text);

        List<Double> result = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            result.add((double) v);
        }

        return result;
    }

    public String generateInsight(String prompt) {
        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }
}
