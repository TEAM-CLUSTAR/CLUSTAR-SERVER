package org.project.domain.ai.rag.G.generate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.project.domain.ai.rag.history.JpaAiCallHistoryWriter;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRagGenerator implements RagGenerator {

    private final ChatClient chatClient;
    private final JpaAiCallHistoryWriter historyWriter;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Override
    public String generate(RagPrompt prompt) {

        String fullPrompt = buildFullPrompt(prompt);
        long startTime = System.currentTimeMillis();

        try {
            var responseSpec = chatClient
                    .prompt()
                    .system("""
                        %s

                        [CONTEXT]
                        %s
                        """.formatted(
                            prompt.systemPrompt(),
                            prompt.context()
                    ))
                    .user(prompt.userPrompt())
                    .advisors(a -> a.param(
                            ChatMemory.CONVERSATION_ID,
                            prompt.conversationId()
                    ))
                    .call()
                    .chatClientResponse();

            String text = extractText(responseSpec);

            long latency = System.currentTimeMillis() - startTime;

            historyWriter.saveSuccess(
                    prompt,
                    fullPrompt,
                    text,
                    latency
            );

            log.info(
                    "AI generation success [conversationId={}, latency={}ms, responseLength={}]",
                    prompt.conversationId(),
                    latency,
                    text.length()
            );

            return text;

        } catch (Exception e) {
            throw e;
        }
    }

    @Recover
    public String recover(Exception e, RagPrompt prompt) {

        String fullPrompt = buildFullPrompt(prompt);

        historyWriter.saveFailure(
                prompt,
                fullPrompt,
                e
        );

        log.error(
                "AI generation failed after retries [conversationId={}, error={}]",
                prompt.conversationId(),
                e.getMessage(),
                e
        );

        throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
    }

    @Override
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String generateForPlan(RagPrompt prompt, String model, Double temperature) {

        return chatClient
                .prompt()
                .options(ChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build()
                )
                .system("""
                %s

                [CONTEXT]
                %s
                """.formatted(
                        prompt.systemPrompt(),
                        prompt.context()
                ))
                .user(prompt.userPrompt())
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        prompt.conversationId()
                ))
                .call()
                .content();
    }

    @Recover
    public String recoverForPlan(Exception e, RagPrompt prompt, String model, Double temperature) {
        log.error("Plan AI 생성 최종 실패 after 3 attempts: {}", e.getMessage(), e);
        throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
    }


    /* =========================
       Helper Methods
       ========================= */

    private String buildFullPrompt(RagPrompt prompt) {
        return """
            [SYSTEM]
            %s

            [CONTEXT]
            %s

            [USER]
            %s
            """.formatted(
                prompt.systemPrompt(),
                prompt.context(),
                prompt.userPrompt()
        );
    }

    private String extractText(ChatClientResponse response) {

        if (response == null || response.chatResponse() == null) {
            throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
        }

        var result = response.chatResponse().getResult();
        if (result == null || result.getOutput() == null) {
            throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
        }

        String text = result.getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new AiException(AiErrorCode.AI_GENERATION_FAILED);
        }

        return text;
    }
}
