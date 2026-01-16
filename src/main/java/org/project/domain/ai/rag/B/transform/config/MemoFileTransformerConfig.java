package org.project.domain.ai.rag.B.transform.config;

import org.springframework.ai.document.DefaultContentFormatter;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.ContentFormatTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MemoFileTransformerConfig {

    @Bean
    public List<DocumentTransformer> memoFileTransformers() {
        return List.of(
                new ContentFormatTransformer(
                        DefaultContentFormatter.defaultConfig()
                ),
                new TokenTextSplitter(
                        800,   // tokens
                        300,   // min chars
                        50,    // min embed length
                        200,   // max chunks
                        true
                )
        );
    }
}

