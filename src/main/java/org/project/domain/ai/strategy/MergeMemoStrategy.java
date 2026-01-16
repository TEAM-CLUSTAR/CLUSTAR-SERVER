package org.project.domain.ai.strategy;

import org.project.domain.ai.dto.BuiltPrompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MergeMemoStrategy implements MemoAiStrategy {

    @Override
    public MemoAiOptions supports() {
        return MemoAiOptions.MERGE;
    }

    @Override
    public BuiltPrompt buildPrompt(
            String context,
            MemoAiOptions option,
            String userPrompt
    ) {

        String system = """
        너는 여러 개의 메모를 하나의 잘 정리된 문서로 통합하는 AI다.
        - 중복된 내용은 제거한다
        - 문맥을 자연스럽게 연결한다
        - 불필요한 개인적 표현은 제거한다
        - 하나의 완성된 문서처럼 작성한다
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
