package org.project.domain.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.review.ai.ReviewAiClient;
import org.project.domain.review.ai.dto.FollowUpInput;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.dto.response.AnswerResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.entity.FindingStatus;
import org.project.domain.review.entity.ReviewMessageRole;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.domainException.ReviewLengthException;
import org.project.global.exception.errorcode.ReviewErrorCode;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewFindingService 테스트")
class ReviewFindingServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long REVIEW_ID = 12L;
    private static final Long FINDING_ID = 101L;

    @InjectMocks
    private ReviewFindingServiceImpl findingService;
    @Mock
    private ReviewWriter reviewWriter;
    @Mock
    private ReviewAiClient reviewAiClient;
    @Spy
    private ReviewProperties reviewProperties = properties();

    @Nested
    @DisplayName("답 보내기")
    class Answer {

        @Test
        @DisplayName("첫 답이면 AI 후속 질문을 만들어 저장하고 함께 돌려준다")
        void firstAnswer_returnsFollowUpQuestion() {
            FollowUpInput input = followUpInput("1~2학년이 많이 사서");
            when(reviewWriter.saveAnswer(USER_ID, REVIEW_ID, FINDING_ID, "1~2학년이 많이 사서"))
                    .thenReturn(new SavedAnswer(message(501L, ReviewMessageRole.USER, "1~2학년이 많이 사서"), FindingStatus.ANSWERING, input));
            when(reviewAiClient.followUp(input)).thenReturn("그 기준으로 보면 어떤 위치에 있나요?");
            when(reviewWriter.saveAiMessage(FINDING_ID, "그 기준으로 보면 어떤 위치에 있나요?"))
                    .thenReturn(message(502L, ReviewMessageRole.AI, "그 기준으로 보면 어떤 위치에 있나요?"));

            AnswerResponse response = findingService.answer(USER_ID, REVIEW_ID, FINDING_ID, "1~2학년이 많이 사서");

            assertThat(response.findingId()).isEqualTo(FINDING_ID);
            assertThat(response.status()).isEqualTo(FindingStatus.ANSWERING);
            assertThat(response.answer().messageId()).isEqualTo(501L);
            assertThat(response.aiMessage().messageId()).isEqualTo(502L);
        }

        @Test
        @DisplayName("두 번째 답부터는 AI를 부르지 않고 aiMessage가 null이다")
        void laterAnswer_noAiCall() {
            when(reviewWriter.saveAnswer(USER_ID, REVIEW_ID, FINDING_ID, "둘째 답"))
                    .thenReturn(new SavedAnswer(message(503L, ReviewMessageRole.USER, "둘째 답"), FindingStatus.ANSWERING, null));

            AnswerResponse response = findingService.answer(USER_ID, REVIEW_ID, FINDING_ID, "둘째 답");

            assertThat(response.aiMessage()).isNull();
            verify(reviewAiClient, never()).followUp(any());
        }

        @Test
        @DisplayName("후속 질문 생성이 실패해도 답은 저장된 채로 aiMessage만 null로 돌려준다")
        void followUpFails_answerStillSaved() {
            FollowUpInput input = followUpInput("첫 답");
            when(reviewWriter.saveAnswer(USER_ID, REVIEW_ID, FINDING_ID, "첫 답"))
                    .thenReturn(new SavedAnswer(message(501L, ReviewMessageRole.USER, "첫 답"), FindingStatus.ANSWERING, input));
            when(reviewAiClient.followUp(input)).thenThrow(new ReviewException(ReviewErrorCode.REVIEW_AI_FAILED));

            AnswerResponse response = findingService.answer(USER_ID, REVIEW_ID, FINDING_ID, "첫 답");

            assertThat(response.answer().messageId()).isEqualTo(501L);
            assertThat(response.aiMessage()).isNull();
            verify(reviewWriter, never()).saveAiMessage(anyLong(), anyString());
        }

        @Test
        @DisplayName("답은 1000자까지 허용하고, 넘으면 ANSWER_TOO_LONG이며 저장하지 않는다")
        void tooLongAnswer_throws() {
            when(reviewWriter.saveAnswer(USER_ID, REVIEW_ID, FINDING_ID, "가".repeat(1000)))
                    .thenReturn(new SavedAnswer(message(501L, ReviewMessageRole.USER, "가".repeat(1000)), FindingStatus.ANSWERING, null));
            findingService.answer(USER_ID, REVIEW_ID, FINDING_ID, "가".repeat(1000));

            assertThatThrownBy(() -> findingService.answer(USER_ID, REVIEW_ID, FINDING_ID, "가".repeat(1001)))
                    .isInstanceOfSatisfying(ReviewLengthException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ReviewErrorCode.ANSWER_TOO_LONG);
                        assertThat(e.getDetail().maxLength()).isEqualTo(1000);
                        assertThat(e.getMessage()).isEqualTo("답변이 너무 길어요. 1000자 이하로 줄여 주세요.");
                    });
            verify(reviewWriter, never()).saveAnswer(anyLong(), anyLong(), anyLong(), org.mockito.ArgumentMatchers.eq("가".repeat(1001)));
        }
    }

    private FollowUpInput followUpInput(String answer) {
        return new FollowUpInput(3L, "제목", "본문", "2. 타깃", "짚은 문장", "짚은 곳", "근거", "첫 질문", answer);
    }

    private ReviewMessageResponse message(Long id, ReviewMessageRole role, String content) {
        return new ReviewMessageResponse(id, role, content, LocalDateTime.now());
    }

    private static ReviewProperties properties() {
        ReviewProperties properties = new ReviewProperties();
        properties.setMaxAnswerLength(1000);
        return properties;
    }
}
