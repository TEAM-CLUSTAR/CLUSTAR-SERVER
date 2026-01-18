package org.project.domain.ai.rag.D.query;

import lombok.RequiredArgsConstructor;
import org.project.domain.ai.dto.request.MemoAiRequest;
import org.project.domain.ai.rag.D.query.dto.RagQuery;
import org.springframework.stereotype.Component;


/**
 * RagQueryHandler
 * - User request → RAG 검색/생성에 필요한 Query로 변환
 * - 향후 intent 분석, query normalization, multi-query 확장 가능
 */
@Component
@RequiredArgsConstructor
public class DefaultRagQueryHandler implements RagQueryHandler {

    @Override
    public RagQuery handle(
            Long userId,
            MemoAiRequest request
    ) {
        return RagQuery.of(
                userId,
                request.option(),
                request.userPrompt(),
                request.memoIds()
        );
    }
}
