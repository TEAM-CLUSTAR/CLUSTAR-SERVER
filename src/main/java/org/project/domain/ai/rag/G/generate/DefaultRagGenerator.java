package org.project.domain.ai.rag.G.generate;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultRagGenerator implements RagGenerator {

    private final ChatClient chatClient;

    @Override
    public String generate(RagPrompt prompt) {

        return chatClient
                .prompt()
                .system(prompt.systemPrompt())
                .user("""
                  %s

                  [CONTEXT]
                  %s
                  """.formatted(
                        prompt.userPrompt(),
                        prompt.context()
                ))
                .call()
                .content();
    }
}
