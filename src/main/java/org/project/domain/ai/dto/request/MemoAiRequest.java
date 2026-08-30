package org.project.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

public record MemoAiRequest(
        // 대화 기록으로 저장되므로 빈 프롬프트는 허용하지 않는다.
        // (기획: 프롬프트 입력 필드에 1글자 이상 입력 시 전송 버튼 활성화)
        @NotBlank
        String userPrompt,
        MemoAiOptions option,
        @NotEmpty
        List<@NotNull Long> memoIds
) {

    public MemoAiRequest {
        option = (option == null) ? MemoAiOptions.DEFAULT : option;
    }

    public static MemoAiRequest of(
            String userPrompt,
            MemoAiOptions option,
            List<Long> memoIds
    ) {
        return new MemoAiRequest(userPrompt, option, memoIds);
    }
}

