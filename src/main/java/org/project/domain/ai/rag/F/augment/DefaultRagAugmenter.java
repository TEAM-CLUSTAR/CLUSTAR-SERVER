package org.project.domain.ai.rag.F.augment;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.project.domain.ai.rag.F.augment.context.RagContextBuilder;
import org.project.domain.ai.rag.F.augment.context.RagContextPolicy;
import org.project.domain.ai.rag.F.augment.dto.RagContextResult;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.project.domain.ai.rag.F.augment.system.SystemPromptResolver;
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
        // System Prompt 결정 (option 기반)
        String systemPrompt =
                systemPromptResolver.resolve(query.option());

        // Context 생성 (retrieved documents)
        RagContextResult contextResult =
                contextBuilder.build(documents);

        // 메모 컨텍스트 검증
        RagContextPolicy.validateContext(contextResult.pureTextLength());

        // RagPrompt 생성
        return RagPrompt.of(
                systemPrompt,
                query.userPrompt(),
                contextResult.context()
        );
    }
}
