package org.project.domain.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.ai.ReviewAiClient;
import org.project.domain.review.ai.dto.AnalysisResult;
import org.project.domain.review.ai.dto.FindingDraft;
import org.project.domain.review.config.ReviewProperties;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.EndReviewResponse;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.FindingCategory;
import org.project.domain.review.entity.FindingStatus;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewMessage;
import org.project.domain.review.entity.ReviewMessageRole;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewMessageRepository;
import org.project.domain.review.repository.ReviewRepository;
import org.project.domain.review.support.ValidFindingDraft;
import org.project.domain.user.entity.User;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.domainException.ReviewLengthException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 테스트")
class ReviewServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long MEMO_ID = 3L;

    @InjectMocks
    private ReviewServiceImpl reviewService;
    @Mock
    private MemoRepository memoRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewFindingRepository findingRepository;
    @Mock
    private ReviewMessageRepository messageRepository;
    @Mock
    private ReviewAiClient reviewAiClient;
    @Mock
    private ReviewWriter reviewWriter;
    @Spy
    private ReviewProperties reviewProperties = properties();

    @Nested
    @DisplayName("검토 시작")
    class StartReview {

        private static final String ANCHOR = "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.";

        @Test
        @DisplayName("본문이 300자(공백 포함) 이상이면 AI 분석 결과를 걸러서 저장하고 돌려준다")
        void enoughContent_analyzesAndSaves() {
            String content = contentOfLength(300);
            givenMyMemo(content);
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());
            when(reviewAiClient.analyze(MEMO_ID, "전공책 중고거래 기획서", content, List.of())).thenReturn(new AnalysisResult(List.of(
                    draft(ANCHOR), draft("본문에 없는 문장.")
            )));
            ReviewResponse saved = new ReviewResponse(12L, 2, List.of());
            when(reviewWriter.saveNewReview(eq(MEMO_ID), eq(content), any())).thenReturn(saved);

            ReviewResponse response = reviewService.startReview(USER_ID, MEMO_ID);

            assertThat(response).isSameAs(saved);
            verify(reviewWriter).saveNewReview(eq(MEMO_ID), eq(content), argThat((List<ValidFindingDraft> drafts) ->
                    drafts.size() == 1 && drafts.get(0).anchorText().equals(ANCHOR)));
        }

        @Test
        @DisplayName("본문이 300자 미만이면 CONTENT_TOO_SHORT이고 AI를 부르지 않는다")
        void shortContent_throws() {
            givenMyMemo(contentOfLength(299));

            assertThatThrownBy(() -> reviewService.startReview(USER_ID, MEMO_ID))
                    .isInstanceOfSatisfying(ReviewLengthException.class, e -> {
                        assertThat(e.getErrorCode()).isEqualTo(ReviewErrorCode.CONTENT_TOO_SHORT);
                        assertThat(e.getDetail().minLength()).isEqualTo(300);
                        assertThat(e.getMessage()).isEqualTo("본문이 짧아 검토할 수 없어요. 300자 이상 작성해 주세요.");
                    });
            verify(reviewAiClient, never()).analyze(anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("이미 검토 중이면 REVIEW_ALREADY_IN_PROGRESS이고 AI를 부르지 않는다")
        void alreadyInProgress_throws() {
            Memo memo = givenMyMemo(contentOfLength(300));
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE))
                    .thenReturn(Optional.of(Review.start(memo, 2, "본문")));

            assertThatThrownBy(() -> reviewService.startReview(USER_ID, MEMO_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
            verify(reviewAiClient, never()).analyze(anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("AI가 빈틈을 줬는데 전부 본문에 없으면 REVIEW_AI_FAILED이고 저장하지 않는다")
        void allDraftsInvalid_throws() {
            String content = contentOfLength(300);
            givenMyMemo(content);
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any()))
                    .thenReturn(new AnalysisResult(List.of(draft("본문에 없는 문장."))));

            assertThatThrownBy(() -> reviewService.startReview(USER_ID, MEMO_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_AI_FAILED);
            verify(reviewWriter, never()).saveNewReview(anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("AI가 빈틈을 찾지 못하면 카드 없는 검토로 저장한다")
        void noFindings_savesEmptyReview() {
            String content = contentOfLength(300);
            givenMyMemo(content);
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any())).thenReturn(new AnalysisResult(List.of()));
            when(reviewWriter.saveNewReview(eq(MEMO_ID), eq(content), eq(List.of())))
                    .thenReturn(new ReviewResponse(12L, 2, List.of()));

            assertThat(reviewService.startReview(USER_ID, MEMO_ID).findings()).isEmpty();
        }

        @Test
        @DisplayName("동시에 두 요청이 같은 버전을 저장하려 하면 REVIEW_ALREADY_IN_PROGRESS")
        void concurrentStart_throws() {
            String content = contentOfLength(300);
            givenMyMemo(content);
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any())).thenReturn(new AnalysisResult(List.of()));
            when(reviewWriter.saveNewReview(anyLong(), anyString(), any()))
                    .thenThrow(new DataIntegrityViolationException("uk_review_memo_version"));

            assertThatThrownBy(() -> reviewService.startReview(USER_ID, MEMO_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
        }

        @Test
        @DisplayName("버전 충돌이 아닌 DB 오류(예: 글자 수 초과)는 '진행 중' 409로 바꾸지 않고 그대로 던진다")
        void otherDataIntegrityError_isNotInProgress() {
            String content = contentOfLength(300);
            givenMyMemo(content);
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any())).thenReturn(new AnalysisResult(List.of()));
            when(reviewWriter.saveNewReview(anyLong(), anyString(), any()))
                    .thenThrow(new DataIntegrityViolationException("value too long for column section_label"));

            assertThatThrownBy(() -> reviewService.startReview(USER_ID, MEMO_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        // 공백을 섞어 정확히 length 글자(제목 제외, 공백 포함)인 본문. 앞에 짚을 문장이 들어 있다
        private String contentOfLength(int length) {
            String base = ANCHOR + "\n";
            return base + "가 ".repeat(length).substring(0, length - base.length());
        }

        private FindingDraft draft(String anchorText) {
            return new FindingDraft("CRITERIA", "2. 타깃", anchorText,
                    "타깃 학년을 고른 기준이 없어요", "짚은 곳", "근거", "질문");
        }
    }

    @Nested
    @DisplayName("다시 검토")
    class RerunReview {

        private static final Long REVIEW_ID = 12L;
        private static final String ANCHOR = "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.";
        private static final String LEFT = "학생회가 사물함 관리를 맡는다.";
        private static final String CONTENT = ANCHOR + "\n" + LEFT + "\n" + "가 ".repeat(150);

        @Test
        @DisplayName("반영 안 한 카드의 문장을 '이미 짚은 문장'으로 넘겨 분석하고, 거른 결과로 새 버전을 저장한다")
        void analyzesWithExcludedAnchorsAndSaves() {
            when(reviewWriter.prepareRerun(USER_ID, REVIEW_ID))
                    .thenReturn(new RerunInput(MEMO_ID, "제목", CONTENT, List.of(LEFT)));
            when(reviewAiClient.analyze(MEMO_ID, "제목", CONTENT, List.of(LEFT)))
                    .thenReturn(new AnalysisResult(List.of(draft(ANCHOR), draft("본문에 없는 문장."))));
            ReviewResponse saved = new ReviewResponse(14L, 3, List.of());
            when(reviewWriter.saveRerun(eq(MEMO_ID), eq(REVIEW_ID), eq(CONTENT), any(), eq(10))).thenReturn(saved);

            assertThat(reviewService.rerunReview(USER_ID, REVIEW_ID)).isSameAs(saved);
            verify(reviewWriter).saveRerun(eq(MEMO_ID), eq(REVIEW_ID), eq(CONTENT), argThat((List<ValidFindingDraft> drafts) ->
                    drafts.size() == 1 && drafts.get(0).anchorText().equals(ANCHOR)), eq(10));
        }

        @Test
        @DisplayName("본문이 300자 미만이면 CONTENT_TOO_SHORT이고 AI를 부르지 않는다")
        void shortContent_throws() {
            when(reviewWriter.prepareRerun(USER_ID, REVIEW_ID))
                    .thenReturn(new RerunInput(MEMO_ID, "제목", "짧은 본문", List.of()));

            assertThatThrownBy(() -> reviewService.rerunReview(USER_ID, REVIEW_ID))
                    .isInstanceOf(ReviewLengthException.class);
            verify(reviewAiClient, never()).analyze(anyLong(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("AI가 준 빈틈이 전부 본문에 없으면 REVIEW_AI_FAILED이고 이전 버전을 건드리지 않는다")
        void allInvalid_throws() {
            when(reviewWriter.prepareRerun(USER_ID, REVIEW_ID))
                    .thenReturn(new RerunInput(MEMO_ID, "제목", CONTENT, List.of()));
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any()))
                    .thenReturn(new AnalysisResult(List.of(draft("본문에 없는 문장."))));

            assertThatThrownBy(() -> reviewService.rerunReview(USER_ID, REVIEW_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_AI_FAILED);
            verify(reviewWriter, never()).saveRerun(anyLong(), anyLong(), anyString(), any(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("동시에 두 번 눌러 같은 버전을 저장하려 하면 REVIEW_ALREADY_IN_PROGRESS")
        void concurrentRerun_throws() {
            when(reviewWriter.prepareRerun(USER_ID, REVIEW_ID))
                    .thenReturn(new RerunInput(MEMO_ID, "제목", CONTENT, List.of()));
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any())).thenReturn(new AnalysisResult(List.of()));
            when(reviewWriter.saveRerun(anyLong(), anyLong(), anyString(), any(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenThrow(new DataIntegrityViolationException("uk_review_memo_version"));

            assertThatThrownBy(() -> reviewService.rerunReview(USER_ID, REVIEW_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
        }

        @Test
        @DisplayName("버전 충돌이 아닌 DB 오류는 '진행 중' 409로 바꾸지 않고 그대로 던진다")
        void otherDataIntegrityError_isNotInProgress() {
            when(reviewWriter.prepareRerun(USER_ID, REVIEW_ID))
                    .thenReturn(new RerunInput(MEMO_ID, "제목", CONTENT, List.of()));
            when(reviewAiClient.analyze(anyLong(), anyString(), anyString(), any())).thenReturn(new AnalysisResult(List.of()));
            when(reviewWriter.saveRerun(anyLong(), anyLong(), anyString(), any(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenThrow(new DataIntegrityViolationException("value too long for column section_label"));

            assertThatThrownBy(() -> reviewService.rerunReview(USER_ID, REVIEW_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        private FindingDraft draft(String anchorText) {
            return new FindingDraft("CRITERIA", "2. 타깃", anchorText, "제목", "짚은 곳", "근거", "질문");
        }
    }

    @Nested
    @DisplayName("현재 검토 조회")
    class GetActiveReview {

        @Test
        @DisplayName("한 번도 검토하지 않았으면 원본 버전 1과 빈 검토를 돌려준다")
        void neverReviewed_returnsVersionOneAndNoReview() {
            givenMyMemo();
            when(reviewRepository.findLatestVersion(MEMO_ID)).thenReturn(Optional.empty());
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());

            ActiveReviewResponse response = reviewService.getActiveReview(USER_ID, MEMO_ID);

            assertThat(response.memoId()).isEqualTo(MEMO_ID);
            assertThat(response.currentVersion()).isEqualTo(1);
            assertThat(response.review()).isNull();
        }

        @Test
        @DisplayName("검토를 종료했으면 마지막 버전 번호와 빈 검토를 돌려준다")
        void onlyEndedReviews_returnsLatestVersionAndNoReview() {
            givenMyMemo();
            when(reviewRepository.findLatestVersion(MEMO_ID)).thenReturn(Optional.of(3));
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());

            ActiveReviewResponse response = reviewService.getActiveReview(USER_ID, MEMO_ID);

            assertThat(response.currentVersion()).isEqualTo(3);
            assertThat(response.review()).isNull();
        }

        @Test
        @DisplayName("진행 중 검토가 있으면 카드마다 대화와 반영 결과를 담아 돌려준다")
        void activeReview_returnsFindingsWithMessages() {
            Memo memo = givenMyMemo();
            Review review = withId(Review.start(memo, 2, "본문"), 12L);
            ReviewFinding applied = withId(finding(review, 1), 101L);
            ReflectionTestUtils.setField(applied, "status", FindingStatus.APPLIED);
            ReflectionTestUtils.setField(applied, "appliedText", "선정 기준: 1~2학년이 기초 전공책을 제일 많이 사기 때문이다.");
            ReviewFinding open = withId(finding(review, 2), 102L);

            when(reviewRepository.findLatestVersion(MEMO_ID)).thenReturn(Optional.of(2));
            when(reviewRepository.findByMemoIdAndStatus(MEMO_ID, ReviewStatus.ACTIVE)).thenReturn(Optional.of(review));
            when(findingRepository.findByReviewIdOrderByNumberAsc(12L)).thenReturn(List.of(applied, open));
            when(messageRepository.findByFindingIdInOrderByIdAsc(List.of(101L, 102L))).thenReturn(List.of(
                    withId(ReviewMessage.of(applied, ReviewMessageRole.USER, "음 1~2학년이 기초 전공책을 제일 많이 사서"), 501L),
                    withId(ReviewMessage.of(applied, ReviewMessageRole.AI, "그 기준으로 보면 어떤 위치에 있나요?"), 502L)
            ));

            ActiveReviewResponse response = reviewService.getActiveReview(USER_ID, MEMO_ID);

            assertThat(response.currentVersion()).isEqualTo(2);
            assertThat(response.review().reviewId()).isEqualTo(12L);
            assertThat(response.review().version()).isEqualTo(2);

            FindingResponse first = response.review().findings().get(0);
            assertThat(first.findingId()).isEqualTo(101L);
            assertThat(first.status()).isEqualTo(FindingStatus.APPLIED);
            assertThat(first.appliedText()).startsWith("선정 기준:");
            assertThat(first.messages()).extracting(m -> m.role().name()).containsExactly("USER", "AI");

            FindingResponse second = response.review().findings().get(1);
            assertThat(second.status()).isEqualTo(FindingStatus.OPEN);
            assertThat(second.messages()).isEmpty();
            assertThat(second.appliedText()).isNull();
        }

        @Test
        @DisplayName("메모가 없거나 휴지통에 있으면 MEMO_NOT_FOUND")
        void memoNotFound_throws() {
            when(memoRepository.findByIdAndNotDeleted(MEMO_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getActiveReview(USER_ID, MEMO_ID))
                    .isInstanceOf(MemoException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemoErrorCode.MEMO_NOT_FOUND);
        }

        @Test
        @DisplayName("남의 메모면 FORBIDDEN_MEMO이고 검토 기록을 조회하지 않는다")
        void othersMemo_throwsForbidden() {
            Memo othersMemo = withId(Memo.createMemo("제목", "본문", user(2L)), MEMO_ID);
            when(memoRepository.findByIdAndNotDeleted(MEMO_ID)).thenReturn(Optional.of(othersMemo));

            assertThatThrownBy(() -> reviewService.getActiveReview(USER_ID, MEMO_ID))
                    .isInstanceOf(MemoException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemoErrorCode.FORBIDDEN_MEMO);
            verify(reviewRepository, never()).findLatestVersion(anyLong());
        }
    }

    @Nested
    @DisplayName("검토 종료")
    class EndReview {

        private static final Long REVIEW_ID = 12L;

        @Test
        @DisplayName("진행 중 검토를 종료로 바꾸고(기록은 남김) 메모 ID와 현재 버전을 돌려준다")
        void endsActiveReview() {
            Review review = withId(Review.start(withId(Memo.createMemo("제목", "본문", user(USER_ID)), MEMO_ID), 3, "본문"), REVIEW_ID);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            EndReviewResponse response = reviewService.endReview(USER_ID, REVIEW_ID);

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.ENDED);
            assertThat(response.memoId()).isEqualTo(MEMO_ID);
            assertThat(response.currentVersion()).isEqualTo(3);
            verify(reviewRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없거나 이미 종료된 검토면 REVIEW_NOT_FOUND")
        void notActive_throws() {
            Review ended = withId(Review.start(withId(Memo.createMemo("제목", "본문", user(USER_ID)), MEMO_ID), 3, "본문"), REVIEW_ID);
            ReflectionTestUtils.setField(ended, "status", ReviewStatus.ENDED);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(ended));
            when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.endReview(USER_ID, REVIEW_ID))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
            assertThatThrownBy(() -> reviewService.endReview(USER_ID, 99L))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("남의 검토면 FORBIDDEN_MEMO이고 종료하지 않는다")
        void othersReview_throws() {
            Review review = withId(Review.start(withId(Memo.createMemo("제목", "본문", user(2L)), MEMO_ID), 3, "본문"), REVIEW_ID);
            when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            assertThatThrownBy(() -> reviewService.endReview(USER_ID, REVIEW_ID))
                    .isInstanceOf(MemoException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemoErrorCode.FORBIDDEN_MEMO);
            assertThat(review.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        }
    }

    private Memo givenMyMemo() {
        return givenMyMemo("본문");
    }

    private Memo givenMyMemo(String content) {
        Memo memo = withId(Memo.createMemo("전공책 중고거래 기획서", content, user(USER_ID)), MEMO_ID);
        when(memoRepository.findByIdAndNotDeleted(MEMO_ID)).thenReturn(Optional.of(memo));
        return memo;
    }

    private static ReviewProperties properties() {
        ReviewProperties properties = new ReviewProperties();
        properties.setMinContentLength(300);
        properties.setMaxFindings(10);
        return properties;
    }

    private User user(Long id) {
        return withId(User.createSocialUser("test@test.com", "테스터", null, "google"), id);
    }

    private ReviewFinding finding(Review review, int number) {
        return ReviewFinding.create(review, number, FindingCategory.CRITERIA, "2. 타깃",
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.",
                "타깃 학년을 고른 기준이 없어요", "짚은 곳", "근거", "질문");
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
