package org.project.domain.ai.rag.F.augment;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultRagAugmenter implements RagAugmenter {

    private final SystemPromptResolver systemPromptResolver;
    private final RagContextBuilder contextBuilder;

    @Override
    public RagPrompt augment(
            RagQuery query,
            List<Document> documents
    ) {
        // 1️⃣ System Prompt 결정 (option 기반)
        String systemPrompt =
                systemPromptResolver.resolve(query.option());

        // 2️⃣ Context 생성 (retrieved documents)
        String context =
                contextBuilder.build(documents);

        // 3️⃣ RagPrompt 생성
        return RagPrompt.of(
                systemPrompt,
                query.userPrompt(),
                context
        );
    }
}
