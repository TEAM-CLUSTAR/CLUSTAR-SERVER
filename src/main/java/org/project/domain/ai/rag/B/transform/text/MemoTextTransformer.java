package org.project.domain.ai.rag.B.transform.text;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemoTextTransformer implements DocumentTransformer {

    private final TokenTextSplitter textSplitter =
            new TokenTextSplitter(
                    800,   // chunk size (tokens)
                    1,   // min chunk chars
                    1,    // min length to embed
                    1000,  // max chunks
                    true   // keep separator (문맥 유지)
            );

    @Override
    public List<Document> apply(List<Document> documents) {
        return textSplitter.apply(documents);
    }

    public List<Document> transform(List<Document> documents) {
        return apply(documents);
    }
}
