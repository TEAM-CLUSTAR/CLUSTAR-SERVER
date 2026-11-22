package org.project.domain.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.dto.response.ActiveReviewResponse;
import org.project.domain.review.dto.response.FindingResponse;
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
import org.project.domain.user.entity.User;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
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

    private Memo givenMyMemo() {
        Memo memo = withId(Memo.createMemo("전공책 중고거래 기획서", "본문", user(USER_ID)), MEMO_ID);
        when(memoRepository.findByIdAndNotDeleted(MEMO_ID)).thenReturn(Optional.of(memo));
        return memo;
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
