package org.project.global.config.ai;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "ai.chat-memory")
public class ChatMemoryProperties {

    /**
     * LLM 프롬프트에 함께 실어 보낼 최근 메시지 수.
     * <p>
     * 초과분은 spring_ai_chat_memory에서 물리 삭제되므로 AI가 기억하는 범위를 결정한다.
     * 화면에 복원되는 대화(chat_message)는 전체가 남으므로, 이 값이 작을수록
     * "화면에는 보이지만 AI는 기억하지 못하는" 구간이 넓어진다.
     * 토큰 비용과 Gemini 쿼터를 고려해 조정한다.
     */
    @Min(2)
    private int maxMessages;
}
