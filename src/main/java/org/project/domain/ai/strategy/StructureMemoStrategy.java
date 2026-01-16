package org.project.domain.ai.strategy;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructureMemoStrategy implements MemoAiStrategy {

    @Override
    public MemoAiOptions supports() {
        return MemoAiOptions.STRUCTURE;
    }

    @Override
    public Prompt buildPrompt(
            List<String> memos,
            MemoAiOptions option,
            String userPrompt
    ) {

        String system = """
        너는 메모를 분석하여 논리적인 문서 구조를 설계하는 AI다.
        - 계층적인 아웃라인을 만든다
        - 1, 1.1, 1.1.1 형태로 표현한다
        - 본문은 작성하지 말고 구조만 출력한다
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

