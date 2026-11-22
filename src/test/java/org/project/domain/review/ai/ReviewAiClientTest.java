package org.project.domain.review.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.ai.dto.FollowUpInput;
import org.project.domain.review.config.ReviewProperties;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("ReviewAiClient 분석 호출 테스트")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ReviewAiClientTest.TestConfig.class)
class ReviewAiClientTest {

    private static final String VALID_JSON = """
            {"findings":[{"category":"CRITERIA","sectionLabel":"2. 타깃",
            "anchorText":"전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.",
            "title":"타깃 학년을 고른 기준이 없어요","reason":"짚은 곳","evidence":"근거",
            "question":"여러 학년 중 1~2학년을 주 사용자로 고른 기준은 무엇인가요?"}]}
            """;

    @Autowired
    private ReviewAiClient reviewAiClient;
    @Autowired
    private ScriptedChatModel chatModel;
    @Autowired
    private ReviewAiCallLogWriter logWriter;

    @BeforeEach
    void setUp() {
        chatModel.reset();
        clearInvocations(logWriter);
    }

    @Test
    @DisplayName("AI가 준 JSON을 빈틈 목록으로 바꾸고, 제목·본문과 분석 설정을 담아 호출한다")
    void analyze_parsesJsonAndSendsDocument() {
        chatModel.respond(VALID_JSON);

        AnalysisResult result = reviewAiClient.analyze(3L, "전공책 중고거래 기획서", "본문 내용");

        assertThat(result.findings()).hasSize(1);
        assertThat(result.findings().get(0).category()).isEqualTo("CRITERIA");
        assertThat(result.findings().get(0).anchorText()).isEqualTo("전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.");

        Prompt prompt = chatModel.lastPrompt();
        assertThat(prompt.getContents()).contains("전공책 중고거래 기획서", "본문 내용");
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) prompt.getOptions();
        assertThat(options.getModel()).isEqualTo("test-model");
        assertThat(options.getTemperature()).isEqualTo(0.2);
        assertThat(options.getResponseMimeType()).isEqualTo("application/json");
        verify(logWriter).success(any(), any(), anyLong());
    }

    @Test
    @DisplayName("첫 응답이 JSON이 아니면 다시 요청해서 성공한다")
    void analyze_retriesOnBrokenJson() {
        chatModel.respond("JSON이 아닌 응답");
        chatModel.respond(VALID_JSON);

        AnalysisResult result = reviewAiClient.analyze(3L, "제목", "본문");

        assertThat(result.findings()).hasSize(1);
        assertThat(chatModel.callCount()).isEqualTo(2);
        verify(logWriter, times(1)).failure(any(), any(), any());
        verify(logWriter, times(1)).success(any(), any(), anyLong());
    }

    @Test
    @DisplayName("3번 모두 실패하면 REVIEW_AI_FAILED")
    void analyze_failsAfterMaxAttempts() {
        chatModel.fail();
        chatModel.fail();
        chatModel.fail();

        assertThatThrownBy(() -> reviewAiClient.analyze(3L, "제목", "본문"))
                .isInstanceOf(ReviewException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.REVIEW_AI_FAILED);
        assertThat(chatModel.callCount()).isEqualTo(3);
        verify(logWriter, times(3)).failure(any(), isNull(), any());
    }

    @Test
    @DisplayName("후속 질문: 첫 질문과 사용자 답을 담아 호출하고 질문 문장을 돌려준다")
    void followUp_returnsQuestion() {
        chatModel.respond("{\"question\":\"그 기준으로 보면, 기준에 맞지 않는 학생은 어떤 위치에 있나요?\"}");

        String question = reviewAiClient.followUp(followUpInput());

        assertThat(question).isEqualTo("그 기준으로 보면, 기준에 맞지 않는 학생은 어떤 위치에 있나요?");
        Prompt prompt = chatModel.lastPrompt();
        assertThat(prompt.getContents()).contains(
                "여러 학년 중 1~2학년을 주 사용자로 고른 기준은 무엇인가요?",
                "음 1~2학년이 기초 전공책을 제일 많이 사서");
        assertThat(((GoogleGenAiChatOptions) prompt.getOptions()).getTemperature()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("후속 질문: 빈 질문이 오거나 계속 실패하면 3번 시도 후 REVIEW_AI_FAILED")
    void followUp_failsAfterMaxAttempts() {
        chatModel.respond("{\"question\":\"  \"}");

        assertThatThrownBy(() -> reviewAiClient.followUp(followUpInput()))
                .isInstanceOf(ReviewException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.REVIEW_AI_FAILED);
        assertThat(chatModel.callCount()).isEqualTo(3);
    }

    private FollowUpInput followUpInput() {
        return new FollowUpInput(3L, "전공책 중고거래 기획서", "본문", "2. 타깃",
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.",
                "짚은 곳", "근거",
                "여러 학년 중 1~2학년을 주 사용자로 고른 기준은 무엇인가요?",
                "음 1~2학년이 기초 전공책을 제일 많이 사서");
    }

    @TestConfiguration
    @EnableRetry
    @Import(ReviewAiClient.class)
    static class TestConfig {
        @Bean
        ScriptedChatModel chatModel() {
            return new ScriptedChatModel();
        }

        @Bean
        ReviewAiCallLogWriter logWriter() {
            return mock(ReviewAiCallLogWriter.class);
        }

        @Bean
        ReviewProperties reviewProperties() {
            ReviewProperties properties = new ReviewProperties();
            properties.setMinContentLength(300);
            properties.setMaxFindings(10);
            properties.getAi().setModel("test-model");
            properties.getAi().setAnalysisTemperature(0.2);
            properties.getAi().setQuestionTemperature(0.3);
            return properties;
        }
    }

    // 정해 둔 순서대로 응답하거나 실패하는 테스트용 모델
    static class ScriptedChatModel implements ChatModel {
        private final Deque<String> script = new ArrayDeque<>();
        private final AtomicInteger callCount = new AtomicInteger();
        private Prompt lastPrompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            callCount.incrementAndGet();
            lastPrompt = prompt;
            String next = script.poll();
            if (next == null) {
                throw new RuntimeException("Gemini 503");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(next))));
        }

        void respond(String text) {
            script.add(text);
        }

        void fail() {
            // 큐가 비어 있으면 실패하므로 아무것도 넣지 않는다
        }

        void reset() {
            script.clear();
            callCount.set(0);
            lastPrompt = null;
        }

        int callCount() {
            return callCount.get();
        }

        Prompt lastPrompt() {
            return lastPrompt;
        }
    }
}
