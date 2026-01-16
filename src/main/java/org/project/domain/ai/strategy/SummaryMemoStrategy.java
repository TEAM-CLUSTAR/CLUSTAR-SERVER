package org.project.domain.ai.strategy;

import org.project.domain.ai.dto.BuiltPrompt;
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
    public BuiltPrompt buildPrompt(
            String context,
            MemoAiOptions option,
            String userPrompt
    ) {

        String system = """
        너는 메모의 핵심만 간결하게 요약하는 AI다.
        - 가장 중요한 포인트만 포함한다
        - 5줄 이내로 작성한다
        - 불필요한 설명은 제거한다
        """;

        String finalPromptText = """
        [SYSTEM]
        %s

        [CONTEXT]
        %s

        [USER]
        %s
        """.formatted(system, context, userPrompt);

        String user = """
        사용자 요청:
        %s

        참고 메모:
        %s
        """.formatted(userPrompt, context);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(system),
                        new UserMessage(user)
                )
        );

        return new BuiltPrompt(prompt, finalPromptText);
    }
}

