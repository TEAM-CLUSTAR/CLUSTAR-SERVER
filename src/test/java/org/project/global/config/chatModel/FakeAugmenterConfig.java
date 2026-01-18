package org.project.global.config.chatModel;

import org.project.domain.ai.rag.F.augment.RagAugmenter;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FakeAugmenterConfig {

    @Bean
    RagAugmenter ragAugmenter() {
        return (query, documents) ->
                RagPrompt.of(
                        "SYSTEM",
                        documents.get(0).getText(),
                        query.userPrompt()
                );
    }
}

