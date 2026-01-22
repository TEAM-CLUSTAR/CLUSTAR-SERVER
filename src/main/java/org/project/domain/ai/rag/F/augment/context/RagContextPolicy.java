package org.project.domain.ai.rag.F.augment.context;

import lombok.NoArgsConstructor;
import org.project.global.exception.InsufficientRagContextException;

@NoArgsConstructor
public final class RagContextPolicy {

    public static final int MIN_CONTEXT_LENGTH = 200;

    public static void validateContext(String context) {
        if (context == null || context.isBlank()) {
            throw new InsufficientRagContextException(
                    "선택된 메모에 유효한 내용이 없습니다."
            );
        }

        if (context.length() < RagContextPolicy.MIN_CONTEXT_LENGTH) {
            throw new InsufficientRagContextException(
                    "선택된 메모의 길이가 너무 짧아 " +
                            "의미 있는 답변을 생성할 수 없습니다."
            );
        }
    }
}

