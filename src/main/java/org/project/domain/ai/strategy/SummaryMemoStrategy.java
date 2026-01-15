package org.project.domain.ai.strategy;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SummaryMemoStrategy implements MemoAiStrategy {

    @Override
    public MemoAiOptions supports() {
        return MemoAiOptions.SUMMARY;
    }

    @Override
    public Prompt buildPrompt(
            List<String> memos,
            MemoAiOptions option,
            String userPrompt
    ) {

        String system = """
        너는 메모의 핵심만 간결하게 요약하는 AI다.
        - 가장 중요한 포인트만 포함한다
        - 5줄 이내로 작성한다
        - 불필요한 설명은 제거한다
        """;

        String memoContent = String.join("\n\n---\n\n", memos);

        String user = """
        사용자 요청:
        %s

        메모 목록:
        %s
        """.formatted(userPrompt, memoContent);

        return new Prompt(
                List.of(
                        new SystemMessage(system),
                        new UserMessage(user)
                )
        );
    }
}

