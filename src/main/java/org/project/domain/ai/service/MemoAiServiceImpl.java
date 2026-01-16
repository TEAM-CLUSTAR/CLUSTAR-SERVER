package org.project.domain.ai.service;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.BuiltPrompt;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.entity.ContextEmbedding;
import org.project.domain.ai.strategy.MemoAiOptions;
import org.project.domain.ai.strategy.MemoAiStrategy;
import org.project.domain.ai.strategy.MemoAiStrategyFactory;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.project.global.util.embedding.RagContextBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoAiServiceImpl implements MemoAiService {

    private final ChatClient chatClient;
    private final MemoAiStrategyFactory strategyFactory;
    private final RagContextBuilder ragContextBuilder;

    private final ContextEmbeddingService embeddingService;
    private final RagSearchService ragSearchService;

    private final MemoRepository memoRepository;


    @Override
    public MemoAiResponse generateMemoAi(Long userId, MemoAiRequest request) {

        MemoAiOptions option = request.option();

        // 전략 선택
        MemoAiStrategy strategy = strategyFactory.get(option);

        float[] queryEmbedding =
                embeddingService.generateEmbedding(request.userPrompt());

        List<ContextEmbedding> chunks =
                ragSearchService.searchRelevantChunks(
                        request.memoIds(),
                        queryEmbedding,
                        6
                );

        String context =
                ragContextBuilder.build(chunks);

        BuiltPrompt builtPrompt =
                strategy.buildPrompt(
                        context,
                        request.option(),
                        request.userPrompt()
                );

        String result =
                chatClient.prompt(builtPrompt.prompt()).call().content();

        return MemoAiResponse.of(
                result,
                request.option(),
                request.memoIds(),
                builtPrompt.promptSnapshot()
        );
    }
}
