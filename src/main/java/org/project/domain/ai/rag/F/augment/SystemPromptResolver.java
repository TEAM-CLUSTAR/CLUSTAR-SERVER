package org.project.domain.ai.rag.F.augment;

import org.project.domain.ai.dto.MemoAiOptions;
import org.springframework.stereotype.Component;

@Component
public class SystemPromptResolver {

    public String resolve(MemoAiOptions option) {
        String titleRule = """
                첫 줄은 메모 제목이므로 제목처럼 짧고 명확하게 작성하세요.
                둘째 줄부터는 본문입니다.
                """;
        return switch (option) {
            case MERGE -> titleRule + """
                    여러 메모의 내용을 하나의 자연스러운 글로 통합하세요.
                    중복은 제거하고 흐름이 자연스럽도록 정리하세요.
                    """;

            case STRUCTURE -> titleRule + """
                    메모 내용을 논리적인 구조로 재정리하세요.
                    제목, 소제목, bullet point를 적극 활용하세요.
                    """;

            case SUMMARY -> titleRule + """
                    메모의 핵심만 간결하게 요약하세요.
                    불필요한 세부 내용은 제거하세요.
                    """;
        };
    }
}
