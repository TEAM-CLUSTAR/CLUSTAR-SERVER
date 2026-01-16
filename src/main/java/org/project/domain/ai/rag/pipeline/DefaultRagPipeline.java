package org.project.domain.ai.rag.pipeline;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.dto.response.MemoAiResponse;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.project.domain.ai.rag.D.query.RagQueryHandler;
import org.project.domain.ai.rag.E.retrieve.RagRetriever;
import org.project.domain.ai.rag.F.augment.RagAugmenter;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.project.domain.ai.rag.G.generate.RagGenerator;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultRagPipeline implements RagPipeline {

    private final RagQueryHandler queryHandler;
    private final RagRetriever retriever;
    private final RagAugmenter augmenter;
    private final RagGenerator generator;

    @Override
    public MemoAiResponse run(
            Long userId,
            MemoAiRequest request
    ) {

        // 1️⃣ Query
        RagQuery query = queryHandler.handle(userId, request);

        // 2️⃣ Retrieve
        List<Document> documents = retriever.retrieve(query);

        // 3️⃣ Augment
        RagPrompt prompt = augmenter.augment(query, documents);

        // 4️⃣ Generate
        String content = generator.generate(prompt);

        // 5️⃣ Response 조립 (파이프라인 책임 OK)
        return MemoAiResponse.of(
                content,
                query.option(),
                query.memoIds(),
                prompt.toDebugString()
        );
    }
}
