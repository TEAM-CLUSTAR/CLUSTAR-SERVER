package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.strategy.MemoAiOptions;
import org.project.domain.ai.strategy.MemoAiStrategy;
import org.project.domain.ai.strategy.MemoAiStrategyFactory;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final MemoAiStrategyFactory strategyFactory;

    private final MemoRepository memoRepository;


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

    @Override
    public MemoAiResponse generateMemoAi(MemoAiRequest request) {

        MemoAiOptions option = request.option();

        // 전략 선택
        MemoAiStrategy strategy = strategyFactory.get(option);

        // 메모 조회
        List<Memo> memos = memoRepository.findAllById(request.memoIds());

        if (memos.isEmpty()) {
            throw new AiException(AiErrorCode.MEMO_NOT_FOUND);
        }

        // 메모 내용 추출
        List<String> memoContents = memos.stream()
                .map(Memo::getContent)
                .toList();

        // Prompt 생성 (Strategy 책임)
        Prompt prompt = strategy.buildPrompt(
                memoContents,
                request.option(),
                request.userPrompt()
        );

        // AI 호출
        String aiResult = chatClient
                .prompt(prompt)
                .call()
                .content();

        // 응답 DTO 생성
        return MemoAiResponse.of(
                aiResult,
                option,
                request.memoIds()
        );
    }
}
