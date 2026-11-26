package org.project.domain.review.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.project.domain.review.ai.dto.ResolvedGap;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.ai.dto.FollowUpInput;
import org.project.domain.review.ai.dto.QuestionAnswers;
import org.project.domain.review.ai.dto.RefineInput;
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

        AnalysisResult result = reviewAiClient.analyze(3L, "전공책 중고거래 기획서", "본문 내용", List.of(), List.of());

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
    @DisplayName("글의 목적(documentGoal)을 먼저 쓰게 하고, 함께 와도 빈틈 목록을 읽는다")
    void analyze_readsDocumentGoalWithFindings() {
        chatModel.respond(VALID_JSON.replaceFirst("\\{", "{\"documentGoal\":\"버려지는 전공책을 싸게 다시 쓰게 한다\","));

        AnalysisResult result = reviewAiClient.analyze(3L, "제목", "본문", List.of(), List.of());

        assertThat(result.documentGoal()).isEqualTo("버려지는 전공책을 싸게 다시 쓰게 한다");
        assertThat(result.findings()).hasSize(1);
        assertThat(chatModel.lastPrompt().getContents()).contains("documentGoal");
    }

    @Test
    @DisplayName("분석 호출은 유형을 세 값으로 묶은 응답 스키마를 함께 보내고, Gemini 스키마로 읽힌다")
    void analyze_sendsSchemaWithCategoryEnum() {
        chatModel.respond(VALID_JSON);

        reviewAiClient.analyze(3L, "제목", "본문", List.of(), List.of());

        String schema = ((GoogleGenAiChatOptions) chatModel.lastPrompt().getOptions()).getResponseSchema();
        assertThat(schema).contains("\"enum\":[\"CRITERIA\",\"STAKEHOLDER\",\"EDGE_CASE\"]").doesNotContain("$schema");
        // 필드별 규칙이 스키마 설명으로 실린다
        assertThat(schema).contains("'~습니다'는 쓰지 않는다", "계획이나 해결책을 요구하지 않는다");
        assertThat(com.google.genai.types.Schema.fromJson(schema).properties()).isPresent();
    }

    @Test
    @DisplayName("후속 질문·다듬기는 응답 스키마를 보내지 않는다")
    void followUp_sendsNoSchema() {
        chatModel.respond("{\"question\":\"그 기준에 맞지 않는 학생은 어떻게 되나요?\"}");

        reviewAiClient.followUp(new FollowUpInput(3L, "제목", "본문", "", "문장", "짚은 곳", "근거", "첫 질문", "답"));

        assertThat(((GoogleGenAiChatOptions) chatModel.lastPrompt().getOptions()).getResponseSchema()).isNull();
    }

    @Test
    @DisplayName("첫 응답이 JSON이 아니면 다시 요청해서 성공한다")
    void analyze_retriesOnBrokenJson() {
        chatModel.respond("JSON이 아닌 응답");
        chatModel.respond(VALID_JSON);

        AnalysisResult result = reviewAiClient.analyze(3L, "제목", "본문", List.of(), List.of());

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

        assertThatThrownBy(() -> reviewAiClient.analyze(3L, "제목", "본문", List.of(), List.of()))
                .isInstanceOf(ReviewException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.REVIEW_AI_FAILED);
        assertThat(chatModel.callCount()).isEqualTo(3);
        verify(logWriter, times(3)).failure(any(), isNull(), any());
    }

    @Test
    @DisplayName("다시 검토: 이미 짚은 문장 목록을 '다시 짚지 말 것'으로 함께 보낸다")
    void analyze_sendsAlreadyFoundAnchors() {
        chatModel.respond("{\"findings\":[]}");

        reviewAiClient.analyze(3L, "제목", "본문", List.of("학생회가 사물함 관리를 맡는다."), List.of());

        assertThat(chatModel.lastPrompt().getContents()).contains("[이미 짚은 문장]", "- 학생회가 사물함 관리를 맡는다.");
    }

    @Test
    @DisplayName("처음 검토는 '이미 짚은 문장' 칸을 넣지 않는다")
    void analyze_firstReviewHasNoAlreadyFoundSection() {
        chatModel.respond("{\"findings\":[]}");

        reviewAiClient.analyze(3L, "제목", "본문", List.of(), List.of());

        assertThat(chatModel.lastPrompt().getContents()).doesNotContain("[이미 짚은 문장]", "[이미 반영한 빈틈]");
    }

    @Test
    @DisplayName("다시 검토: 이미 반영한 빈틈(제목·짚은 문장·넣은 문장)을 보내, 해결됐으면 같은 빈틈을 다시 내지 않도록 알린다")
    void analyze_sendsResolvedGaps() {
        chatModel.respond("{\"findings\":[]}");

        reviewAiClient.analyze(3L, "제목", "본문", List.of(), List.of(new ResolvedGap(
                "회비 미납 시 처리 방안이 없어요", "회비는 MT 일주일 전까지 총무 계좌로 받는다.", "미납자는 MT에 참가할 수 없다.")));

        assertThat(chatModel.lastPrompt().getContents()).contains(
                "[이미 반영한 빈틈]",
                "회비 미납 시 처리 방안이 없어요",
                "회비는 MT 일주일 전까지 총무 계좌로 받는다.",
                "미납자는 MT에 참가할 수 없다.",
                "해결했으면 같은 빈틈을 다시 내지 않는다");
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
    @DisplayName("후속 질문: 답이 첫 질문과 동떨어지면 넓히지 말고 첫 질문의 핵심을 다시 묻도록 알린다")
    void followUp_tellsToReaskWhenAnswerIsOffTopic() {
        chatModel.respond("{\"question\":\"그럼 MT는 언제 갈 예정인가요?\"}");

        reviewAiClient.followUp(followUpInput());

        assertThat(chatModel.lastPrompt().getContents()).contains(
                "[답이 첫 질문과 동떨어졌을 때]",
                "첫 질문이 묻는 핵심을 더 쉽고 구체적으로 다시 묻는다");
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

    @Test
    @DisplayName("다듬기: 질문별 라벨과 문장을 '라벨: 문장' 줄로 이어 돌려주고, 질문과 답을 담아 호출한다")
    void refine_joinsLabeledLines() {
        chatModel.respond("""
                {"lines":[{"label":"선정 기준","sentence":"1~2학년이 기초 전공책을 제일 많이 사기 때문이다."},
                {"label":"그 외 학생","sentence":"3~4학년도 살 수 있지만, 홍보는 저학년 위주로 한다."}]}
                """);

        String refined = reviewAiClient.refine(refineInput());

        assertThat(refined).isEqualTo("선정 기준: 1~2학년이 기초 전공책을 제일 많이 사기 때문이다.\n"
                + "그 외 학생: 3~4학년도 살 수 있지만, 홍보는 저학년 위주로 한다.");
        Prompt prompt = chatModel.lastPrompt();
        assertThat(prompt.getContents()).contains("음 1~2학년이 기초 전공책을 제일 많이 사서", "3~4학년도 살 수는 있는데 홍보는 저학년 위주로 할 듯");
        assertThat(((GoogleGenAiChatOptions) prompt.getOptions()).getTemperature()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("다듬기: 답에 없던 숫자가 나오면 버리고 다시 요청한다")
    void refine_retriesWhenNewNumberAppears() {
        chatModel.respond("{\"lines\":[{\"label\":\"선정 기준\",\"sentence\":\"1~2학년이 전체 구매의 70%를 차지하기 때문이다.\"}]}");
        chatModel.respond("{\"lines\":[{\"label\":\"선정 기준\",\"sentence\":\"1~2학년이 기초 전공책을 제일 많이 사기 때문이다.\"}]}");

        String refined = reviewAiClient.refine(refineInput());

        assertThat(refined).isEqualTo("선정 기준: 1~2학년이 기초 전공책을 제일 많이 사기 때문이다.");
        assertThat(chatModel.callCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("다듬기: 질문 수만큼, 질문 순서대로 한 줄씩 만들라고 개수를 알려 준다")
    void refine_tellsLineCountMatchingQuestions() {
        chatModel.respond("{\"lines\":[{\"label\":\"선정 기준\",\"sentence\":\"1~2학년이 많이 사기 때문이다.\"}]}");

        reviewAiClient.refine(refineInput());

        assertThat(chatModel.lastPrompt().getContents()).contains("질문이 2개이므로 lines도 정확히 2개");
    }

    @Test
    @DisplayName("다듬기: 답의 숫자 일부만 떼어 쓴 숫자(1000 → 100)는 답에 없던 숫자로 보고 다시 요청한다")
    void refine_rejectsNumberThatIsOnlyPartOfAnswerNumber() {
        chatModel.respond("{\"lines\":[{\"label\":\"예산\",\"sentence\":\"예산은 100원이다.\"}]}");
        chatModel.respond("{\"lines\":[{\"label\":\"예산\",\"sentence\":\"예산은 1000원이다.\"}]}");

        String refined = reviewAiClient.refine(singleAnswerInput("예산은 1000원"));

        assertThat(refined).isEqualTo("예산: 예산은 1000원이다.");
        assertThat(chatModel.callCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("다듬기: 15%를 5%로 바꾼 것도 답에 없던 숫자로 본다")
    void refine_rejectsChangedPercent() {
        chatModel.respond("{\"lines\":[{\"label\":\"수수료\",\"sentence\":\"수수료는 5%이다.\"}]}");
        chatModel.respond("{\"lines\":[{\"label\":\"수수료\",\"sentence\":\"수수료는 15%이다.\"}]}");

        String refined = reviewAiClient.refine(singleAnswerInput("수수료 15%"));

        assertThat(refined).isEqualTo("수수료: 수수료는 15%이다.");
        assertThat(chatModel.callCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("다듬기: 1,000과 1000처럼 쉼표만 다른 숫자는 같은 숫자로 본다")
    void refine_acceptsSameNumberWithoutComma() {
        chatModel.respond("{\"lines\":[{\"label\":\"예산\",\"sentence\":\"예산은 1000원이다.\"}]}");

        String refined = reviewAiClient.refine(singleAnswerInput("예산은 1,000원"));

        assertThat(refined).isEqualTo("예산: 예산은 1000원이다.");
        assertThat(chatModel.callCount()).isEqualTo(1);
    }

    private RefineInput singleAnswerInput(String answer) {
        return new RefineInput(3L, "4. 운영", "수수료는 받지 않고 학생회 예산으로 운영한다.", List.of(
                new QuestionAnswers("운영 비용은 어떻게 마련하나요?", List.of(answer))
        ));
    }

    private RefineInput refineInput() {
        return new RefineInput(3L, "2. 타깃", "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.", List.of(
                new QuestionAnswers("여러 학년 중 1~2학년을 주 사용자로 고른 기준은 무엇인가요?",
                        List.of("음 1~2학년이 기초 전공책을 제일 많이 사서")),
                new QuestionAnswers("그 기준으로 보면, 기준에 맞지 않는 학생은 어떤 위치에 있나요?",
                        List.of("3~4학년도 살 수는 있는데 홍보는 저학년 위주로 할 듯"))
        ));
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
            properties.getAi().setRefineTemperature(0.1);
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
