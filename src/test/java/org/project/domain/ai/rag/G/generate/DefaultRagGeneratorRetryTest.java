package org.project.domain.ai.rag.G.generate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;
import org.project.domain.ai.rag.history.JpaAiCallHistoryWriter;
import org.project.global.exception.domainException.AiException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("DefaultRagGenerator 재시도 테스트")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DefaultRagGeneratorRetryTest.TestConfig.class)
class DefaultRagGeneratorRetryTest {

    @Autowired
    private RagGenerator generator;

    @Autowired
    private CountingFailingChatModel chatModel;

    @BeforeEach
    void setUp() {
        chatModel.reset();
    }

    @Test
    @DisplayName("2번 실패 후 3번째 시도에서 성공하면 정상 응답을 반환한다")
    void generate_retries_and_succeeds() {
        // given
        RagPrompt prompt = RagPrompt.of("system", "user", "context")
                .withConversationContext(1L, 1L);
        chatModel.setFailTimes(2);

        // when
        String result = generator.generate(prompt);

        // then
        assertThat(result).isEqualTo("OK");
        assertThat(chatModel.getCallCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("최대 재시도 횟수를 초과하면 AiException을 발생시킨다")
    void generate_retries_and_throws_after_max_attempts() {
        // given
        RagPrompt prompt = RagPrompt.of("system", "user", "context")
                .withConversationContext(1L, 1L);
        chatModel.setFailTimes(3);

        // when & then
        assertThatThrownBy(() -> generator.generate(prompt))
                .isInstanceOf(AiException.class);

        assertThat(chatModel.getCallCount()).isEqualTo(3);
    }

    @TestConfiguration
    @EnableRetry // @Retryable 어노테이션 활성화
    @Import(DefaultRagGenerator.class)
    static class TestConfig {

        @Bean
        CountingFailingChatModel chatModel() {
            return new CountingFailingChatModel();
        }

        @Bean
        ChatClient chatClient(ChatModel chatModel) {
            return ChatClient.builder(chatModel).build();
        }

        @Bean
        JpaAiCallHistoryWriter historyWriter() {
            return mock(JpaAiCallHistoryWriter.class);
        }
    }

    // 테스트용 Mock 객체
    static class CountingFailingChatModel implements ChatModel {

        private final AtomicInteger callCount = new AtomicInteger();
        private final AtomicInteger failTimes = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            int count = callCount.incrementAndGet();
            if (count <= failTimes.get()) {
                throw new RuntimeException("boom");
            }

            AssistantMessage message = new AssistantMessage("OK");
            Generation generation = new Generation(message);
            return new ChatResponse(List.of(generation));
        }

        void setFailTimes(int times) {
            failTimes.set(times);
        }

        int getCallCount() {
            return callCount.get();
        }

        void reset() {
            callCount.set(0);
            failTimes.set(0);
        }
    }
}
