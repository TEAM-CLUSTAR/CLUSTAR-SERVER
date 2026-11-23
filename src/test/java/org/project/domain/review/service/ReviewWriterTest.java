package org.project.domain.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.memo.event.MemoTextUpdatedEvent;
import org.project.domain.review.ai.dto.QuestionAnswers;
import org.project.domain.review.dto.request.ApplyType;
import org.project.domain.review.dto.response.ApplyResponse;
import org.project.domain.review.ai.dto.RefineInput;
import org.project.domain.review.dto.response.FindingResponse;
import org.project.domain.review.dto.response.ReviewMessageResponse;
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
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@RecordApplicationEvents
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

    @Test
    @DisplayName("분석하는 사이 메모가 휴지통으로 갔으면 MEMO_IN_TRASH이고 검토를 저장하지 않는다")
    void trashedWhileAnalyzing_throws() {
        Memo memo = saveMemo();
        memoRepository.moveToTrash(memo.getId(), LocalDateTime.now());

        assertThatThrownBy(() -> reviewWriter.saveNewReview(memo.getId(), "본문", List.of(draft("첫 문장."))))
                .isInstanceOf(ReviewException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.MEMO_IN_TRASH);
        assertThat(reviewRepository.findLatestVersion(memo.getId())).isEmpty();
    }

    @Nested
    @DisplayName("답 저장")
    class SaveAnswer {

        @Autowired
        private ReviewMessageRepository messageRepository;
        @Autowired
        private TestEntityManager em;

        @Test
        @DisplayName("첫 답이면 답을 저장하고 '답하는 중'으로 바꾸며, 후속 질문에 쓸 재료를 돌려준다")
        void firstAnswer_savesAndReturnsFollowUpInput() {
            ReviewFinding finding = saveActiveFinding();

            SavedAnswer saved = reviewWriter.saveAnswer(userIdOf(finding), reviewIdOf(finding), finding.getId(), "1~2학년이 제일 많이 사서");

            assertThat(saved.answer().role()).isEqualTo(ReviewMessageRole.USER);
            assertThat(saved.answer().content()).isEqualTo("1~2학년이 제일 많이 사서");
            assertThat(saved.status()).isEqualTo(FindingStatus.ANSWERING);
            assertThat(saved.followUpInput()).isNotNull();
            assertThat(saved.followUpInput().question()).isEqualTo("질문");
            assertThat(saved.followUpInput().answer()).isEqualTo("1~2학년이 제일 많이 사서");
            assertThat(saved.followUpInput().memoContent()).isEqualTo("본문");
            assertThat(findingRepository.findById(finding.getId()).orElseThrow().getStatus()).isEqualTo(FindingStatus.ANSWERING);
        }

        @Test
        @DisplayName("두 번째 답부터는 후속 질문 재료를 돌려주지 않는다")
        void secondAnswer_hasNoFollowUpInput() {
            ReviewFinding finding = saveActiveFinding();
            reviewWriter.saveAnswer(userIdOf(finding), reviewIdOf(finding), finding.getId(), "첫 답");

            SavedAnswer saved = reviewWriter.saveAnswer(userIdOf(finding), reviewIdOf(finding), finding.getId(), "둘째 답");

            assertThat(saved.followUpInput()).isNull();
            assertThat(messageRepository.findByFindingIdInOrderByIdAsc(List.of(finding.getId())))
                    .extracting(ReviewMessage::getContent).containsExactly("첫 답", "둘째 답");
        }

        @Test
        @DisplayName("AI 후속 질문을 대화에 저장한다")
        void saveAiMessage() {
            ReviewFinding finding = saveActiveFinding();

            ReviewMessageResponse message = reviewWriter.saveAiMessage(finding.getId(), "후속 질문?");

            assertThat(message.role()).isEqualTo(ReviewMessageRole.AI);
            assertThat(message.content()).isEqualTo("후속 질문?");
        }

        @Test
        @DisplayName("질문을 기다리는 사이 카드를 반영했으면 질문을 저장하지 않고 null을 돌려준다")
        void saveAiMessage_appliedFinding_returnsNull() {
            ReviewFinding finding = saveActiveFinding();
            finding.apply("반영한 문장");

            ReviewMessageResponse message = reviewWriter.saveAiMessage(finding.getId(), "후속 질문?");

            assertThat(message).isNull();
            assertThat(messageRepository.countByFindingIdAndRole(finding.getId(), ReviewMessageRole.AI)).isZero();
        }

        @Test
        @DisplayName("질문을 기다리는 사이 검토가 끝났으면(종료·다시 검토) 질문을 저장하지 않고 null을 돌려준다")
        void saveAiMessage_endedReview_returnsNull() {
            ReviewFinding finding = saveActiveFinding();
            finding.getReview().end();

            ReviewMessageResponse message = reviewWriter.saveAiMessage(finding.getId(), "후속 질문?");

            assertThat(message).isNull();
            assertThat(messageRepository.countByFindingIdAndRole(finding.getId(), ReviewMessageRole.AI)).isZero();
        }

        @Test
        @DisplayName("다른 검토의 카드면 FINDING_NOT_FOUND")
        void findingOfAnotherReview_throws() {
            ReviewFinding finding = saveActiveFinding();

            assertThatThrownBy(() -> reviewWriter.saveAnswer(userIdOf(finding), 9999L, finding.getId(), "답"))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.FINDING_NOT_FOUND);
        }

        @Test
        @DisplayName("종료된 검토면 REVIEW_NOT_FOUND")
        void endedReview_throws() {
            ReviewFinding finding = saveActiveFinding();
            ReflectionTestUtils.setField(finding.getReview(), "status", ReviewStatus.ENDED);
            em.flush();

            assertThatThrownBy(() -> reviewWriter.saveAnswer(userIdOf(finding), reviewIdOf(finding), finding.getId(), "답"))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("남의 검토면 FORBIDDEN_MEMO")
        void othersReview_throws() {
            ReviewFinding finding = saveActiveFinding();

            assertThatThrownBy(() -> reviewWriter.saveAnswer(userIdOf(finding) + 1, reviewIdOf(finding), finding.getId(), "답"))
                    .isInstanceOf(MemoException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemoErrorCode.FORBIDDEN_MEMO);
        }

        @Test
        @DisplayName("메모가 휴지통에 있으면 MEMO_NOT_FOUND")
        void trashedMemo_throws() {
            ReviewFinding finding = saveActiveFinding();
            Long userId = userIdOf(finding);
            Long reviewId = reviewIdOf(finding);
            memoRepository.moveToTrash(finding.getReview().getMemo().getId(), LocalDateTime.now());

            assertThatThrownBy(() -> reviewWriter.saveAnswer(userId, reviewId, finding.getId(), "답"))
                    .isInstanceOf(MemoException.class)
                    .extracting("errorCode")
                    .isEqualTo(MemoErrorCode.MEMO_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 반영한 카드면 FINDING_ALREADY_APPLIED")
        void appliedFinding_throws() {
            ReviewFinding finding = saveActiveFinding();
            ReflectionTestUtils.setField(finding, "status", FindingStatus.APPLIED);
            em.flush();

            assertThatThrownBy(() -> reviewWriter.saveAnswer(userIdOf(finding), reviewIdOf(finding), finding.getId(), "답"))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.FINDING_ALREADY_APPLIED);
        }
    }

    @Nested
    @DisplayName("다듬기 재료 준비")
    class PrepareRefine {

        @Autowired
        private ReviewMessageRepository messageRepository;

        @Test
        @DisplayName("답을 질문별로 묶는다: 후속 질문 전의 답은 첫 질문에, 후의 답은 후속 질문에")
        void groupsAnswersByQuestion() {
            ReviewFinding finding = saveActiveFinding();
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "음 1~2학년이 많이 사서"));
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.AI, "그 기준으로 보면 어떤 위치에 있나요?"));
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "3~4학년도 살 수는 있음"));
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "홍보는 저학년 위주로 할 듯"));

            RefineInput input = reviewWriter.prepareRefine(userIdOf(finding), reviewIdOf(finding), finding.getId());

            assertThat(input.anchorText()).isEqualTo("전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.");
            assertThat(input.questionAnswers()).containsExactly(
                    new QuestionAnswers("질문", List.of("음 1~2학년이 많이 사서")),
                    new QuestionAnswers("그 기준으로 보면 어떤 위치에 있나요?",
                            List.of("3~4학년도 살 수는 있음", "홍보는 저학년 위주로 할 듯"))
            );
            assertThat(input.rawText()).isEqualTo("음 1~2학년이 많이 사서\n3~4학년도 살 수는 있음\n홍보는 저학년 위주로 할 듯");
        }

        @Test
        @DisplayName("답하지 않은 후속 질문은 빼고 묶는다")
        void skipsUnansweredQuestion() {
            ReviewFinding finding = saveActiveFinding();
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "첫 답"));
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.AI, "후속 질문?"));

            RefineInput input = reviewWriter.prepareRefine(userIdOf(finding), reviewIdOf(finding), finding.getId());

            assertThat(input.questionAnswers()).containsExactly(new QuestionAnswers("질문", List.of("첫 답")));
        }

        @Test
        @DisplayName("답이 하나도 없으면 ANSWER_REQUIRED")
        void noAnswers_throws() {
            ReviewFinding finding = saveActiveFinding();

            assertThatThrownBy(() -> reviewWriter.prepareRefine(userIdOf(finding), reviewIdOf(finding), finding.getId()))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.ANSWER_REQUIRED);
        }
    }

    @Nested
    @DisplayName("문서에 반영")
    class Apply {

        private static final String ANCHOR = "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.";
        private static final String CONTENT = "2. 타깃\n" + ANCHOR + "\n판매자는 졸업 예정자와 3~4학년이다.";

        @Autowired
        private ReviewMessageRepository messageRepository;
        @Autowired
        private ApplicationEvents events;

        @Test
        @DisplayName("다듬은 문장을 짚은 문장 다음 줄에 넣고, 카드를 반영됨으로 바꾸고, 재임베딩 이벤트를 낸다")
        void refined_insertsIntoMemoAndMarksApplied() {
            ReviewFinding finding = saveFindingInMemo(CONTENT);

            ApplyResponse response = reviewWriter.apply(userIdOf(finding), reviewIdOf(finding), finding.getId(),
                    ApplyType.REFINED, "선정 기준: 1~2학년이 많이 사기 때문이다.");

            String expected = "2. 타깃\n" + ANCHOR + "\n선정 기준: 1~2학년이 많이 사기 때문이다.\n판매자는 졸업 예정자와 3~4학년이다.";
            assertThat(response.content()).isEqualTo(expected);
            assertThat(memoRepository.findById(response.memoId()).orElseThrow().getContent()).isEqualTo(expected);
            assertThat(response.finding().status()).isEqualTo(FindingStatus.APPLIED);
            assertThat(response.finding().appliedText()).isEqualTo("선정 기준: 1~2학년이 많이 사기 때문이다.");
            assertThat(findingRepository.findById(finding.getId()).orElseThrow().isApplied()).isTrue();
            assertThat(events.stream(MemoTextUpdatedEvent.class))
                    .containsExactly(new MemoTextUpdatedEvent(response.memoId(), userIdOf(finding)));
        }

        @Test
        @DisplayName("원문 그대로 넣기는 보낸 text와 상관없이 저장된 내 답을 순서대로 이어 넣는다")
        void raw_insertsStoredAnswers() {
            ReviewFinding finding = saveFindingInMemo(CONTENT);
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "음 1~2학년이 많이 사서"));
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.AI, "후속 질문?"));
            messageRepository.save(ReviewMessage.of(finding, ReviewMessageRole.USER, "홍보는 저학년 위주"));

            ApplyResponse response = reviewWriter.apply(userIdOf(finding), reviewIdOf(finding), finding.getId(),
                    ApplyType.RAW, "무시되는 문장");

            assertThat(response.finding().appliedText()).isEqualTo("음 1~2학년이 많이 사서\n홍보는 저학년 위주");
            assertThat(response.content()).contains(ANCHOR + "\n음 1~2학년이 많이 사서\n홍보는 저학년 위주\n");
        }

        @Test
        @DisplayName("원문 그대로 넣기인데 답이 없으면 ANSWER_REQUIRED")
        void rawWithoutAnswers_throws() {
            ReviewFinding finding = saveFindingInMemo(CONTENT);

            assertThatThrownBy(() -> reviewWriter.apply(userIdOf(finding), reviewIdOf(finding), finding.getId(), ApplyType.RAW, null))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.ANSWER_REQUIRED);
        }

        @Test
        @DisplayName("본문이 바뀌어 짚은 문장을 못 찾으면 ANCHOR_NOT_FOUND이고 반영하지 않는다")
        void anchorMissing_throws() {
            ReviewFinding finding = saveFindingInMemo("2. 타깃\n누군가 고쳐 쓴 문장.");

            assertThatThrownBy(() -> reviewWriter.apply(userIdOf(finding), reviewIdOf(finding), finding.getId(), ApplyType.REFINED, "문장"))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.ANCHOR_NOT_FOUND);
            assertThat(finding.isApplied()).isFalse();
            assertThat(events.stream(MemoTextUpdatedEvent.class)).isEmpty();
        }

        @Test
        @DisplayName("이미 반영한 카드면 FINDING_ALREADY_APPLIED(두 번 눌러도 한 번만 들어감)")
        void appliedTwice_throws() {
            ReviewFinding finding = saveFindingInMemo(CONTENT);
            reviewWriter.apply(userIdOf(finding), reviewIdOf(finding), finding.getId(), ApplyType.REFINED, "문장");

            assertThatThrownBy(() -> reviewWriter.apply(userIdOf(finding), reviewIdOf(finding), finding.getId(), ApplyType.REFINED, "문장"))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.FINDING_ALREADY_APPLIED);
        }

        private ReviewFinding saveFindingInMemo(String content) {
            User user = userRepository.save(User.createSocialUser("test@test.com", "테스터", null, "google"));
            Memo memo = memoRepository.save(Memo.createMemo("전공책 중고거래 기획서", content, user));
            Review review = reviewRepository.save(Review.start(memo, 2, content));
            return findingRepository.save(ReviewFinding.create(review, 1, FindingCategory.CRITERIA, "2. 타깃",
                    ANCHOR, "제목", "짚은 곳", "근거", "질문"));
        }
    }

    @Nested
    @DisplayName("다시 검토")
    class Rerun {

        private static final String A = "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.";
        private static final String B = "판매자는 졸업 예정자와 3~4학년이다.";
        private static final String D = "거래는 학생회관 사물함을 통한 비대면 방식으로 진행한다.";
        private static final String C = "학생회가 사물함 관리를 맡는다.";
        private static final String CONTENT = String.join("\n",
                "2. 타깃", A, "선정 기준: 많이 사기 때문이다.", B, D, C);

        @Autowired
        private ReviewMessageRepository messageRepository;

        private Review oldReview;
        private ReviewFinding findingB;

        // v2: A는 반영함, B는 답하는 중(대화 2개), C는 아직 안 건드림
        private void givenVersionTwo() {
            User user = userRepository.save(User.createSocialUser("test@test.com", "테스터", null, "google"));
            Memo memo = memoRepository.save(Memo.createMemo("전공책 중고거래 기획서", CONTENT, user));
            oldReview = reviewRepository.save(Review.start(memo, 2, "v2 때 본문"));

            ReviewFinding findingA = finding(oldReview, 1, A);
            findingA.apply("선정 기준: 많이 사기 때문이다.");
            findingB = finding(oldReview, 2, B);
            findingB.markAnswering();
            findingRepository.saveAll(List.of(findingA, findingB, finding(oldReview, 3, C)));
            messageRepository.save(ReviewMessage.of(findingB, ReviewMessageRole.USER, "책이 남는 학년이라서"));
            messageRepository.save(ReviewMessage.of(findingB, ReviewMessageRole.AI, "그럼 2학년은 어떤가요?"));
        }

        @Test
        @DisplayName("준비: 현재 본문과, 반영하지 않은 카드의 문장만 '이미 짚은 문장'으로 돌려준다")
        void prepare_returnsUnappliedAnchors() {
            givenVersionTwo();

            RerunInput input = reviewWriter.prepareRerun(userIdOf(findingB), oldReview.getId());

            assertThat(input.content()).isEqualTo(CONTENT);
            assertThat(input.title()).isEqualTo("전공책 중고거래 기획서");
            assertThat(input.excludedAnchors()).containsExactly(B, C);
        }

        @Test
        @DisplayName("저장: 이전 버전은 종료로 남기고, 반영 안 한 카드는 대화째 복사, 새 빈틈은 추가, 본문 순서로 번호를 다시 매긴다")
        void save_createsNextVersion() {
            givenVersionTwo();

            ReviewResponse response = reviewWriter.saveRerun(memoIdOf(oldReview), oldReview.getId(), CONTENT,
                    List.of(draft(D), draft(B)), 10);

            assertThat(response.version()).isEqualTo(3);
            assertThat(response.reviewId()).isNotEqualTo(oldReview.getId());
            assertThat(response.findings()).extracting(FindingResponse::anchorText).containsExactly(B, D, C);
            assertThat(response.findings()).extracting(FindingResponse::number).containsExactly(1, 2, 3);

            FindingResponse copiedB = response.findings().get(0);
            assertThat(copiedB.findingId()).isNotEqualTo(findingB.getId());
            assertThat(copiedB.status()).isEqualTo(FindingStatus.ANSWERING);
            assertThat(copiedB.messages()).extracting(ReviewMessageResponse::content)
                    .containsExactly("책이 남는 학년이라서", "그럼 2학년은 어떤가요?");
            assertThat(response.findings().get(1).status()).isEqualTo(FindingStatus.OPEN);

            Review saved = reviewRepository.findById(response.reviewId()).orElseThrow();
            assertThat(saved.isActive()).isTrue();
            assertThat(saved.getContentSnapshot()).isEqualTo(CONTENT);
            assertThat(oldReview.getStatus()).isEqualTo(ReviewStatus.ENDED);
            // 이전 버전 기록은 그대로
            assertThat(findingRepository.findByReviewIdOrderByNumberAsc(oldReview.getId())).hasSize(3);
            assertThat(messageRepository.countByFindingIdAndRole(findingB.getId(), ReviewMessageRole.USER)).isEqualTo(1);
        }

        @Test
        @DisplayName("저장: 남은 카드와 새 빈틈을 합쳐 최대 개수를 넘지 않는다(남은 카드가 우선)")
        void save_respectsMaxFindings() {
            givenVersionTwo();

            ReviewResponse response = reviewWriter.saveRerun(memoIdOf(oldReview), oldReview.getId(), CONTENT, List.of(draft(D)), 2);

            assertThat(response.findings()).extracting(FindingResponse::anchorText).containsExactly(B, C);
        }

        @Test
        @DisplayName("이미 종료된 검토면 REVIEW_NOT_FOUND")
        void endedReview_throws() {
            givenVersionTwo();
            oldReview.end();

            assertThatThrownBy(() -> reviewWriter.saveRerun(memoIdOf(oldReview), oldReview.getId(), CONTENT, List.of(), 10))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
            assertThatThrownBy(() -> reviewWriter.prepareRerun(userIdOf(findingB), oldReview.getId()))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("분석하는 사이 메모가 휴지통으로 갔으면 MEMO_IN_TRASH이고 이전 버전은 진행 중으로 남는다")
        void trashedWhileAnalyzing_throws() {
            givenVersionTwo();
            Long memoId = memoIdOf(oldReview);
            memoRepository.moveToTrash(memoId, LocalDateTime.now());

            assertThatThrownBy(() -> reviewWriter.saveRerun(memoId, oldReview.getId(), CONTENT, List.of(draft(D)), 10))
                    .isInstanceOf(ReviewException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.MEMO_IN_TRASH);
            assertThat(reviewRepository.findById(oldReview.getId()).orElseThrow().isActive()).isTrue();
            assertThat(reviewRepository.findLatestVersion(memoId)).contains(2);
        }

        private ReviewFinding finding(Review review, int number, String anchor) {
            return ReviewFinding.create(review, number, FindingCategory.CRITERIA, "2. 타깃",
                    anchor, "제목 " + number, "짚은 곳", "근거", "질문");
        }
    }

    private ReviewFinding saveActiveFinding() {
        Review review = reviewRepository.save(Review.start(saveMemo(), 2, "본문"));
        return findingRepository.save(ReviewFinding.create(review, 1, FindingCategory.CRITERIA, "2. 타깃",
                "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.", "제목", "짚은 곳", "근거", "질문"));
    }

    private Long userIdOf(ReviewFinding finding) {
        return finding.getReview().getMemo().getUser().getId();
    }

    private Long memoIdOf(Review review) {
        return review.getMemo().getId();
    }

    private Long reviewIdOf(ReviewFinding finding) {
        return finding.getReview().getId();
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
