package org.project.domain.ai.rag.F.augment.dto;

public record RagContextResult(
        String context,        // [MEMO] 포함된 RAG CONTEXT
        int pureTextLength     // [MEMO] 제거된 순수 메모 텍스트 글자 수
) {
}
