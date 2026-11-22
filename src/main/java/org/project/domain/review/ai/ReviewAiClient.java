package org.project.domain.review.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.ai.dto.FollowUpInput;
import org.project.domain.review.ai.dto.FollowUpResult;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.entity.ReviewAiCallLog.ReviewAiCall;
import org.project.domain.review.entity.ReviewAiCallType;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

// 검토 전용 AI 호출.
// 공용 ChatClient 빈에는 대화 메모리(MessageChatMemoryAdvisor)가 붙어 있어 쓰지 않고 ChatModel을 직접 부른다.
// 검토는 필요한 맥락(본문, 카드, 답)을 매번 프롬프트에 직접 넣으므로 대화 메모리가 필요 없다
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAiClient {

    private static final String JSON_MIME_TYPE = "application/json";

    private final ChatModel chatModel;
    private final ReviewProperties properties;
    private final ReviewAiCallLogWriter logWriter;

    private final BeanOutputConverter<AnalysisResult> analysisConverter = new BeanOutputConverter<>(AnalysisResult.class);
    private final BeanOutputConverter<FollowUpResult> followUpConverter = new BeanOutputConverter<>(FollowUpResult.class);

    // 실패하거나 JSON이 깨지면 1초 → 2초 간격으로 최대 3번 시도한다(기존 RAG 생성과 같은 정책)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public AnalysisResult analyze(Long memoId, String title, String content) {
        ReviewAiCall call = new ReviewAiCall(
                memoId,
                ReviewAiCallType.ANALYSIS,
                properties.getAi().getModel(),
                ReviewPrompts.analysisSystem(properties.getMaxFindings()),
                ReviewPrompts.analysisUser(title, content, analysisConverter.getFormat())
        );
        return request(call, properties.getAi().getAnalysisTemperature(), this::parseAnalysis);
    }

    @Recover
    public AnalysisResult recoverAnalyze(Exception e, Long memoId, String title, String content) {
        log.error("AI 검토 분석 실패(재시도 소진) [memoId={}, error={}]", memoId, e.getMessage(), e);
        throw new ReviewException(ReviewErrorCode.REVIEW_AI_FAILED);
    }

    // 사용자의 첫 답을 보고 후속 질문 하나를 만든다
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String followUp(FollowUpInput input) {
        ReviewAiCall call = new ReviewAiCall(
                input.memoId(),
                ReviewAiCallType.FOLLOW_UP,
                properties.getAi().getModel(),
                ReviewPrompts.followUpSystem(),
                ReviewPrompts.followUpUser(
                        input.memoTitle(),
                        input.memoContent(),
                        input.sectionLabel(),
                        input.anchorText(),
                        input.reason(),
                        input.evidence(),
                        input.question(),
                        input.answer(),
                        followUpConverter.getFormat()
                )
        );
        return request(call, properties.getAi().getQuestionTemperature(), this::parseFollowUp);
    }

    @Recover
    public String recoverFollowUp(Exception e, FollowUpInput input) {
        log.warn("AI 후속 질문 생성 실패(재시도 소진) [memoId={}, error={}]", input.memoId(), e.getMessage());
        throw new ReviewException(ReviewErrorCode.REVIEW_AI_FAILED);
    }

    // 한 번 호출하고, 응답을 해석하고, 성공/실패를 기록한다
    private <T> T request(ReviewAiCall call, double temperature, Function<String, T> parser) {
        long startedAt = System.currentTimeMillis();
        String responseText = null;
        try {
            ChatResponse response = chatModel.call(new Prompt(
                    List.of(new SystemMessage(call.systemPrompt()), new UserMessage(call.userPrompt())),
                    GoogleGenAiChatOptions.builder()
                            .model(call.model())
                            .temperature(temperature)
                            .responseMimeType(JSON_MIME_TYPE)
                            .build()
            ));
            responseText = extractText(response);
            T parsed = parser.apply(responseText);
            logWriter.success(call, responseText, System.currentTimeMillis() - startedAt);
            return parsed;
        } catch (RuntimeException e) {
            logWriter.failure(call, responseText, e);
            throw e;
        }
    }

    private AnalysisResult parseAnalysis(String text) {
        AnalysisResult result = analysisConverter.convert(text);
        if (result == null || result.findings() == null) {
            throw new IllegalStateException("findings가 없는 분석 응답");
        }
        return result;
    }

    private String parseFollowUp(String text) {
        FollowUpResult result = followUpConverter.convert(text);
        if (result == null || result.question() == null || result.question().isBlank()) {
            throw new IllegalStateException("질문이 없는 후속 질문 응답");
        }
        return result.question().trim();
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("AI 응답이 비어 있음");
        }
        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("AI 응답이 비어 있음");
        }
        return text;
    }
}
