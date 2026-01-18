package org.project.domain.memo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MemoAiCreateRequest(
        @NotBlank(message = "AI 메모 제목은 필수입니다.")
        String title,

        @NotBlank(message = "AI 메모 내용은 필수입니다.")
        String content,

        @NotEmpty(message = "참고한 메모 ID는 필수입니다.")
        List<@NotNull Long> sourceMemoIds
) {
}
