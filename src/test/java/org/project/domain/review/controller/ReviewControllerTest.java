package org.project.domain.review.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.review.dto.request.ApplyType;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.ApplyResponse;
import org.project.domain.review.dto.response.EndReviewResponse;
import org.project.domain.review.dto.response.AnswerResponse;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.RefineResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.FindingCategory;
import org.project.domain.review.entity.FindingStatus;
import org.project.domain.review.entity.ReviewMessageRole;
import org.project.domain.review.service.ReviewFindingService;
import org.project.domain.review.service.ReviewService;
import org.project.domain.user.dto.CustomUserDetails;
import org.project.domain.user.entity.User;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.domainException.ReviewLengthException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.http.MediaType;
import org.project.global.security.filter.JWTFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReviewController 테스트")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ReviewService reviewService;
    @MockBean
    private ReviewFindingService findingService;
    @MockBean
    private JWTFilter jwtFilter;

    @Test
    @DisplayName("진행 중 검토가 없으면 review 필드를 null로 내려준다")
    @WithMockCustomUser
    void getActiveReview_noReview() throws Exception {
        when(reviewService.getActiveReview(1L, 3L)).thenReturn(new ActiveReviewResponse(3L, 1, null));

        mockMvc.perform(get("/api/v1/ai/reviews/active").param("memoId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memoId").value(3))
                .andExpect(jsonPath("$.data.currentVersion").value(1))
                .andExpect(jsonPath("$.data.review").value(nullValue()));
    }

    @Test
    @DisplayName("진행 중 검토가 있으면 카드와 대화를 함께 내려준다")
    @WithMockCustomUser
    void getActiveReview_withReview() throws Exception {
        FindingResponse finding = new FindingResponse(
                101L, 1, FindingCategory.CRITERIA, "2. 타깃",
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.",
                "타깃 학년을 고른 기준이 없어요", "짚은 곳", "근거",
                "여러 학년 중 1~2학년을 주 사용자로 고른 기준은 무엇인가요?",
                FindingStatus.ANSWERING,
                List.of(new ReviewMessageResponse(501L, ReviewMessageRole.USER, "음 1~2학년이 많이 사서", LocalDateTime.now())),
                null
        );
        when(reviewService.getActiveReview(1L, 3L)).thenReturn(
                new ActiveReviewResponse(3L, 2, new ReviewResponse(12L, 2, List.of(finding))));

        mockMvc.perform(get("/api/v1/ai/reviews/active").param("memoId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(2))
                .andExpect(jsonPath("$.data.review.reviewId").value(12))
                .andExpect(jsonPath("$.data.review.version").value(2))
                .andExpect(jsonPath("$.data.review.findings[0].category").value("CRITERIA"))
                .andExpect(jsonPath("$.data.review.findings[0].status").value("ANSWERING"))
                .andExpect(jsonPath("$.data.review.findings[0].messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.review.findings[0].appliedText").value(nullValue()));
    }

    @Test
    @DisplayName("메모가 없으면 404")
    @WithMockCustomUser
    void getActiveReview_memoNotFound() throws Exception {
        when(reviewService.getActiveReview(1L, 3L)).thenThrow(new MemoException(MemoErrorCode.MEMO_NOT_FOUND));

        mockMvc.perform(get("/api/v1/ai/reviews/active").param("memoId", "3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("검토를 시작하면 201과 새 버전의 카드 목록을 내려준다")
    @WithMockCustomUser
    void startReview_created() throws Exception {
        FindingResponse finding = new FindingResponse(
                101L, 1, FindingCategory.CRITERIA, "2. 타깃",
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.",
                "타깃 학년을 고른 기준이 없어요", "짚은 곳", "근거", "질문",
                FindingStatus.OPEN, List.of(), null
        );
        when(reviewService.startReview(1L, 3L)).thenReturn(new ReviewResponse(12L, 2, List.of(finding)));

        mockMvc.perform(post("/api/v1/ai/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memoId\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.reviewId").value(12))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.findings[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.findings[0].messages").isEmpty());
    }

    @Test
    @DisplayName("memoId가 없으면 400이고 서비스를 부르지 않는다")
    @WithMockCustomUser
    void startReview_missingMemoId() throws Exception {
        mockMvc.perform(post("/api/v1/ai/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(reviewService, never()).startReview(anyLong(), anyLong());
    }

    @Test
    @DisplayName("이미 검토 중이면 409")
    @WithMockCustomUser
    void startReview_alreadyInProgress() throws Exception {
        when(reviewService.startReview(1L, 3L))
                .thenThrow(new ReviewException(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS));

        mockMvc.perform(post("/api/v1/ai/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memoId\": 3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("첫 답을 보내면 내 답과 AI 후속 질문을 함께 내려준다")
    @WithMockCustomUser
    void answer_withFollowUp() throws Exception {
        when(findingService.answer(1L, 12L, 101L, "1~2학년이 많이 사서")).thenReturn(new AnswerResponse(
                101L, FindingStatus.ANSWERING,
                new ReviewMessageResponse(501L, ReviewMessageRole.USER, "1~2학년이 많이 사서", LocalDateTime.now()),
                new ReviewMessageResponse(502L, ReviewMessageRole.AI, "그 기준으로 보면 어떤 위치에 있나요?", LocalDateTime.now())
        ));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"1~2학년이 많이 사서\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.findingId").value(101))
                .andExpect(jsonPath("$.data.status").value("ANSWERING"))
                .andExpect(jsonPath("$.data.answer.role").value("USER"))
                .andExpect(jsonPath("$.data.aiMessage.role").value("AI"));
    }

    @Test
    @DisplayName("두 번째 답부터는 aiMessage를 null로 내려준다")
    @WithMockCustomUser
    void answer_withoutFollowUp() throws Exception {
        when(findingService.answer(1L, 12L, 101L, "둘째 답")).thenReturn(new AnswerResponse(
                101L, FindingStatus.ANSWERING,
                new ReviewMessageResponse(503L, ReviewMessageRole.USER, "둘째 답", LocalDateTime.now()),
                null
        ));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"둘째 답\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiMessage").value(nullValue()));
    }

    @Test
    @DisplayName("빈 답이면 400이고 서비스를 부르지 않는다")
    @WithMockCustomUser
    void answer_blank() throws Exception {
        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"   \"}"))
                .andExpect(status().isBadRequest());
        verify(findingService, never()).answer(anyLong(), anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("답이 너무 길면 400과 함께 최대 글자 수를 알려준다(현재 글자 수는 알려주지 않음)")
    @WithMockCustomUser
    void answer_tooLong() throws Exception {
        when(findingService.answer(1L, 12L, 101L, "긴 답"))
                .thenThrow(ReviewLengthException.answerTooLong(1000));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"긴 답\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("답변이 너무 길어요. 1000자 이하로 줄여 주세요."))
                .andExpect(jsonPath("$.data.currentLength").doesNotExist())
                .andExpect(jsonPath("$.data.maxLength").value(1000))
                .andExpect(jsonPath("$.data.minLength").doesNotExist());
    }

    @Test
    @DisplayName("본문이 짧으면 400과 함께 최소 글자 수를 알려준다(현재 글자 수는 알려주지 않음)")
    @WithMockCustomUser
    void startReview_contentTooShort() throws Exception {
        when(reviewService.startReview(1L, 3L))
                .thenThrow(ReviewLengthException.contentTooShort(300));

        mockMvc.perform(post("/api/v1/ai/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memoId\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("본문이 짧아 검토할 수 없어요. 300자 이상 작성해 주세요."))
                .andExpect(jsonPath("$.data.currentLength").doesNotExist())
                .andExpect(jsonPath("$.data.minLength").value(300));
    }

    @Test
    @DisplayName("다듬기는 다듬은 문장과 내 답 원문을 내려준다")
    @WithMockCustomUser
    void refine_ok() throws Exception {
        when(findingService.refine(1L, 12L, 101L)).thenReturn(new RefineResponse(
                101L, "선정 기준: 1~2학년이 많이 사기 때문이다.", "음 1~2학년이 많이 사서"));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/refine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.findingId").value(101))
                .andExpect(jsonPath("$.data.refinedText").value("선정 기준: 1~2학년이 많이 사기 때문이다."))
                .andExpect(jsonPath("$.data.rawText").value("음 1~2학년이 많이 사서"));
    }

    @Test
    @DisplayName("답이 없으면 다듬기는 400")
    @WithMockCustomUser
    void refine_noAnswers() throws Exception {
        when(findingService.refine(1L, 12L, 101L)).thenThrow(new ReviewException(ReviewErrorCode.ANSWER_REQUIRED));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/refine"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("다듬기 AI가 실패하면 503")
    @WithMockCustomUser
    void refine_aiFailed() throws Exception {
        when(findingService.refine(1L, 12L, 101L)).thenThrow(new ReviewException(ReviewErrorCode.REVIEW_AI_FAILED));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/refine"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503));
    }

    @Test
    @DisplayName("반영하면 바뀐 본문 전체와 반영된 카드 정보를 내려준다")
    @WithMockCustomUser
    void apply_ok() throws Exception {
        when(findingService.apply(1L, 12L, 101L, ApplyType.REFINED, "선정 기준: 많이 사기 때문이다.")).thenReturn(new ApplyResponse(
                3L, "2. 타깃\n짚은 문장\n선정 기준: 많이 사기 때문이다.", LocalDateTime.of(2026, 11, 22, 15, 0),
                new ApplyResponse.AppliedFinding(101L, FindingStatus.APPLIED, "선정 기준: 많이 사기 때문이다.")));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"REFINED\", \"text\": \"선정 기준: 많이 사기 때문이다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memoId").value(3))
                .andExpect(jsonPath("$.data.content").value("2. 타깃\n짚은 문장\n선정 기준: 많이 사기 때문이다."))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.finding.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.finding.appliedText").value("선정 기준: 많이 사기 때문이다."));
    }

    @Test
    @DisplayName("반영 방식(type)이 없으면 400이고 서비스를 부르지 않는다")
    @WithMockCustomUser
    void apply_missingType() throws Exception {
        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"문장\"}"))
                .andExpect(status().isBadRequest());
        verify(findingService, never()).apply(anyLong(), anyLong(), anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("본문에서 짚은 문장을 못 찾으면 409")
    @WithMockCustomUser
    void apply_anchorNotFound() throws Exception {
        when(findingService.apply(1L, 12L, 101L, ApplyType.REFINED, "문장"))
                .thenThrow(new ReviewException(ReviewErrorCode.ANCHOR_NOT_FOUND));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"REFINED\", \"text\": \"문장\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("다시 검토하면 201과 새 버전(새 reviewId)의 카드 목록을 내려준다")
    @WithMockCustomUser
    void rerun_created() throws Exception {
        when(reviewService.rerunReview(1L, 12L)).thenReturn(new ReviewResponse(14L, 3, List.of()));

        mockMvc.perform(post("/api/v1/ai/reviews/12/rerun"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewId").value(14))
                .andExpect(jsonPath("$.data.version").value(3));
    }

    @Test
    @DisplayName("이미 종료된 검토를 다시 검토하면 404")
    @WithMockCustomUser
    void rerun_notFound() throws Exception {
        when(reviewService.rerunReview(1L, 12L)).thenThrow(new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));

        mockMvc.perform(post("/api/v1/ai/reviews/12/rerun"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("검토를 종료하면 메모 ID와 현재 버전을 내려준다")
    @WithMockCustomUser
    void end_ok() throws Exception {
        when(reviewService.endReview(1L, 12L)).thenReturn(new EndReviewResponse(3L, 3));

        mockMvc.perform(post("/api/v1/ai/reviews/12/end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memoId").value(3))
                .andExpect(jsonPath("$.data.currentVersion").value(3));
    }

    @Test
    @DisplayName("이미 종료된 검토면 404")
    @WithMockCustomUser
    void end_notFound() throws Exception {
        when(reviewService.endReview(1L, 12L)).thenThrow(new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND));

        mockMvc.perform(post("/api/v1/ai/reviews/12/end"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("이미 반영한 카드면 409")
    @WithMockCustomUser
    void answer_alreadyApplied() throws Exception {
        when(findingService.answer(1L, 12L, 101L, "답"))
                .thenThrow(new ReviewException(ReviewErrorCode.FINDING_ALREADY_APPLIED));

        mockMvc.perform(post("/api/v1/ai/reviews/12/findings/101/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"답\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
    @interface WithMockCustomUser {
        long userId() default 1L;
    }

    static class WithMockCustomUserSecurityContextFactory
            implements WithSecurityContextFactory<WithMockCustomUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            User user = User.builder()
                    .id(annotation.userId())
                    .email("test@example.com")
                    .name("테스트유저")
                    .providerName("google")
                    .build();
            CustomUserDetails userDetails = new CustomUserDetails(user);
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
            return context;
        }
    }
}
