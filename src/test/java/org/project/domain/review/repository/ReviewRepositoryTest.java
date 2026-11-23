package org.project.domain.review.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.entity.FindingCategory;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewAiCallLog;
import org.project.domain.review.entity.ReviewAiCallLog.ReviewAiCall;
import org.project.domain.review.entity.ReviewAiCallType;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewMessage;
import org.project.domain.review.entity.ReviewMessageRole;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("Review 레포지토리 테스트")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewFindingRepository findingRepository;
    @Autowired
    private ReviewMessageRepository messageRepository;
    @Autowired
    private MemoRepository memoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewAiCallLogRepository callLogRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("검토한 적이 없는 메모는 최신 버전이 비어 있다")
    void findLatestVersion_noReview_returnsEmpty() {
        Memo memo = saveMemo();

        assertThat(reviewRepository.findLatestVersion(memo.getId())).isEmpty();
    }

    @Test
    @DisplayName("최신 버전은 종료 여부와 상관없이 가장 큰 버전 번호다")
    void findLatestVersion_returnsMaxVersion() {
        Memo memo = saveMemo();
        reviewRepository.save(ended(Review.start(memo, 2, "본문 v2")));
        reviewRepository.save(ended(Review.start(memo, 3, "본문 v3")));

        assertThat(reviewRepository.findLatestVersion(memo.getId())).contains(3);
    }

    @Test
    @DisplayName("진행 중 검토 조회는 종료된 버전을 건너뛴다")
    void findByMemoIdAndStatus_skipsEnded() {
        Memo memo = saveMemo();
        reviewRepository.save(ended(Review.start(memo, 2, "본문 v2")));
        Review active = reviewRepository.save(Review.start(memo, 3, "본문 v3"));

        assertThat(reviewRepository.findByMemoIdAndStatus(memo.getId(), ReviewStatus.ACTIVE))
                .get()
                .extracting(Review::getId)
                .isEqualTo(active.getId());
    }

    @Test
    @DisplayName("카드는 번호순, 대화는 저장순으로 조회된다")
    void findingsAndMessages_areOrdered() {
        Review review = reviewRepository.save(Review.start(saveMemo(), 2, "본문"));
        ReviewFinding second = findingRepository.save(finding(review, 2));
        ReviewFinding first = findingRepository.save(finding(review, 1));
        messageRepository.save(ReviewMessage.of(first, ReviewMessageRole.USER, "첫 답"));
        messageRepository.save(ReviewMessage.of(first, ReviewMessageRole.AI, "후속 질문"));

        List<ReviewFinding> findings = findingRepository.findByReviewIdOrderByNumberAsc(review.getId());
        List<ReviewMessage> messages = messageRepository.findByFindingIdInOrderByIdAsc(
                List.of(first.getId(), second.getId()));

        assertThat(findings).extracting(ReviewFinding::getNumber).containsExactly(1, 2);
        assertThat(messages).extracting(ReviewMessage::getContent).containsExactly("첫 답", "후속 질문");
    }

    // 기존 영구 삭제 코드는 그대로 두고 DB의 ON DELETE CASCADE로 검토 기록을 함께 지운다
    @Test
    @DisplayName("메모를 영구 삭제하면 그 메모의 검토, 카드, 대화가 함께 삭제된다")
    void purgeMemo_cascadesReviewData() {
        Memo memo = saveMemo();
        Review review = reviewRepository.save(Review.start(memo, 2, "본문"));
        ReviewFinding finding = findingRepository.save(finding(review, 1));
        messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "답"));
        callLogRepository.save(ReviewAiCallLog.success(
                new ReviewAiCall(memo.getId(), ReviewAiCallType.ANALYSIS, "model", "system", "본문과 답이 담긴 요청"),
                "응답", 10L));
        em.flush();

        memoRepository.moveToTrash(memo.getId(), LocalDateTime.now());
        memoRepository.deleteTrashedByIdIn(List.of(memo.getId()));

        assertThat(reviewRepository.count()).isZero();
        assertThat(findingRepository.count()).isZero();
        assertThat(messageRepository.count()).isZero();
        assertThat(callLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("소제목과 카드 제목이 255자를 넘어도 그대로 저장된다")
    void longSectionLabelAndTitle_areSaved() {
        Review review = reviewRepository.save(Review.start(saveMemo(), 2, "본문"));
        String longText = "가".repeat(300);

        findingRepository.save(ReviewFinding.create(review, 1, FindingCategory.CRITERIA, longText,
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.", longText, "짚은 곳", "근거", "질문"));
        em.flush();
        em.clear();

        ReviewFinding saved = findingRepository.findByReviewIdOrderByNumberAsc(review.getId()).get(0);
        assertThat(saved.getSectionLabel()).isEqualTo(longText);
        assertThat(saved.getTitle()).isEqualTo(longText);
    }

    private Memo saveMemo() {
        User user = userRepository.save(User.createSocialUser("test@test.com", "테스터", null, "google"));
        return memoRepository.save(Memo.createMemo("전공책 중고거래 기획서", "본문", user));
    }

    private Review ended(Review review) {
        ReflectionTestUtils.setField(review, "status", ReviewStatus.ENDED);
        return review;
    }

    private ReviewFinding finding(Review review, int number) {
        return ReviewFinding.create(review, number, FindingCategory.CRITERIA, "2. 타깃",
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.",
                "타깃 학년을 고른 기준이 없어요", "짚은 곳", "근거", "질문");
    }
}
