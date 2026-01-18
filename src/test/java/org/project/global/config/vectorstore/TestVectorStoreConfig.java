package org.project.global.config.vectorstore;

import org.mockito.Mockito;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestVectorStoreConfig {

    @Bean
    @Primary
    public VectorStore vectorStore() {
        return Mockito.mock(VectorStore.class);
    }
}
