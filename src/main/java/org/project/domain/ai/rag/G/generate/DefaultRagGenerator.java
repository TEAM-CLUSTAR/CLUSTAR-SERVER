package org.project.domain.ai.rag.G.generate;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultRagGenerator implements RagGenerator {

    private final ChatClient chatClient;

    @Override
    public String generate(RagPrompt prompt) {

        return chatClient
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
                .content();
    }

    @Override
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
}
