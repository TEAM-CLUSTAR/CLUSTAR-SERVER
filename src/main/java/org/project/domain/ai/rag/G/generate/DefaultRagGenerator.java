package org.project.domain.ai.rag.G.generate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;
import org.springframework.ai.chat.client.ChatClient;
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

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Override
    public String generate(RagPrompt prompt) {

        try {
            var response = chatClient
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

            log.debug("Advisor context: {}", response.context());
            return response.chatResponse().getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("AI 호출 실패", e);
            throw e;
        }
    }

    @Recover
    public String recover(Exception e, RagPrompt prompt) {
        log.error("AI 생성 최종 실패 after 3 attempts: {}", e.getMessage(), e);
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
}
