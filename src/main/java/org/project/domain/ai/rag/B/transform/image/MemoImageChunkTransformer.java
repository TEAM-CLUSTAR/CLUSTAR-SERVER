package org.project.domain.ai.rag.B.transform.image;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoImageChunkTransformer implements DocumentTransformer {

    private final TokenTextSplitter splitter =
            new TokenTextSplitter(
                    600,  // chunk size
                    1,  // min chars
                    1,   // min embed length
                    20,   // max chunks
                    true
            );

    @Override
    public List<Document> apply(List<Document> documents) {
        return splitter.apply(documents);
    }
}

