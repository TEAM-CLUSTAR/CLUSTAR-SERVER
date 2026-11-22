package org.project.domain.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.dto.response.ReviewResponse;
import org.project.domain.review.entity.FindingCategory;
import org.project.domain.review.entity.FindingStatus;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewRepository;
import org.project.domain.review.support.ValidFindingDraft;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslTestConfig.class, ReviewWriter.class})
@DisplayName("ReviewWriter 테스트")
class ReviewWriterTest {

    @Autowired
    private ReviewWriter reviewWriter;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewFindingRepository findingRepository;
    @Autowired
    private MemoRepository memoRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("처음 검토하면 v2로 저장하고, 본문 스냅샷과 번호 매긴 카드를 함께 저장한다")
    void firstReview_savesVersionTwoWithNumberedFindings() {
        Memo memo = saveMemo();

        ReviewResponse response = reviewWriter.saveNewReview(memo.getId(), "분석한 본문", List.of(
                draft("첫 문장."), draft("둘째 문장.")
        ));

        assertThat(response.version()).isEqualTo(2);
        assertThat(response.findings()).extracting(f -> f.number()).containsExactly(1, 2);
        assertThat(response.findings()).allSatisfy(f -> {
            assertThat(f.findingId()).isNotNull();
            assertThat(f.status()).isEqualTo(FindingStatus.OPEN);
            assertThat(f.messages()).isEmpty();
        });

        Review saved = reviewRepository.findById(response.reviewId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        assertThat(saved.getContentSnapshot()).isEqualTo("분석한 본문");
        assertThat(findingRepository.findByReviewIdOrderByNumberAsc(saved.getId())).hasSize(2);
    }

    @Test
    @DisplayName("종료한 v3가 있으면 다음 검토는 v4가 된다")
    void afterEndedVersion_continuesNumbering() {
        Memo memo = saveMemo();
        Review ended = Review.start(memo, 3, "이전 본문");
        ReflectionTestUtils.setField(ended, "status", ReviewStatus.ENDED);
        reviewRepository.save(ended);

        ReviewResponse response = reviewWriter.saveNewReview(memo.getId(), "본문", List.of());

        assertThat(response.version()).isEqualTo(4);
        assertThat(response.findings()).isEmpty();
    }

    @Test
    @DisplayName("분석하는 사이 다른 요청이 먼저 검토를 만들었으면 REVIEW_ALREADY_IN_PROGRESS")
    void activeReviewExists_throws() {
        Memo memo = saveMemo();
        reviewRepository.save(Review.start(memo, 2, "본문"));

        assertThatThrownBy(() -> reviewWriter.saveNewReview(memo.getId(), "본문", List.of()))
                .isInstanceOf(ReviewException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
    }

    private Memo saveMemo() {
        User user = userRepository.save(User.createSocialUser("test@test.com", "테스터", null, "google"));
        return memoRepository.save(Memo.createMemo("제목", "본문", user));
    }

    private ValidFindingDraft draft(String anchorText) {
        return new ValidFindingDraft(FindingCategory.CRITERIA, "2. 타깃", anchorText,
                "제목", "짚은 곳", "근거", "질문");
    }
}
