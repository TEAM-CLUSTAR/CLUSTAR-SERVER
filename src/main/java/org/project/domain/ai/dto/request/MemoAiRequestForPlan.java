package org.project.domain.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.project.domain.ai.dto.MemoAiOptions;

import java.util.List;

@Schema(description = "플랜 기반 메모 AI 요청 DTO")
public record MemoAiRequestForPlan(

        @Schema(
                description = "사용자가 입력한 요청 프롬프트",
                example = "유니온파인드 기말시험 공부용으로 핵심만 정리해줘"
        )
        String userPrompt,

        @NotNull
        @Schema(
                description = "AI 처리 옵션",
                example = "MERGE"
        )
        MemoAiOptions option,

        @NotEmpty
        @Schema(
                description = "AI가 참조할 메모 ID 목록",
                example = "[43, 44, 45]"
        )
        List<@NotNull Long> memoIds,

        @NotNull
        @Schema(
                description = "플랜에서 정의한 System Prompt",
                example = """
                        너는 컴퓨터공학 전공 시험 대비를 돕는 AI 튜터다.
                        - 핵심 개념 위주로 정리한다
                        - 시간 복잡도와 목적을 명확히 설명한다
                        - 불필요한 장황한 설명은 피한다
                        """
        )
        String systemPrompt,

        @NotNull
        @Schema(
                description = "모델 정의",
                example = "gemini-3-flash-preview"
        )
        String model,

        @NotNull
        @Schema(
                description = "temperature 정의",
                example = "0.7"
        )
        Double temperature
) {

    public static MemoAiRequestForPlan of(
            String userPrompt,
            MemoAiOptions option,
            List<Long> memoIds,
            String systemPrompt,
            String model,
            Double temperature
    ) {
        return new MemoAiRequestForPlan(userPrompt, option, memoIds, systemPrompt, model, temperature);
    }
}
