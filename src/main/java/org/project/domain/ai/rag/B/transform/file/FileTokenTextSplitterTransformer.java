package org.project.domain.ai.rag.B.transform.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(20)
public class FileTokenTextSplitterTransformer implements DocumentTransformer {

    private final TokenTextSplitter splitter =
            new TokenTextSplitter(
                    800,   // chunk size (tokens)
                    300,   // min chars
                    50,    // min length to embed
                    10_000,
                    true
            );

    @Override
    public List<Document> apply(List<Document> documents) {
        log.info("[FileTransformer] Splitting {} documents", documents.size());
        return splitter.apply(documents);
    }
}
