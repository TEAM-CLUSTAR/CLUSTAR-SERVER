package org.project.domain.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.repository.MemoRepository;
import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewStatus;
import org.project.domain.review.repository.ReviewRepository;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.vectorstore.TestVectorStoreConfig;
import org.project.global.exception.domainException.ReviewException;
import org.project.global.exception.errorcode.ReviewErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.project.domain.review.dto.response.ReviewMessageResponse;
import org.project.domain.review.entity.FindingCategory;
import org.project.domain.review.entity.ReviewFinding;
import org.project.domain.review.entity.ReviewMessageRole;
import org.project.domain.review.repository.ReviewFindingRepository;
import org.project.domain.review.repository.ReviewMessageRepository;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.atomic.AtomicReference;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 실제로 두 트랜잭션을 겹쳐 돌려, 저장이 메모 잠금을 기다린 뒤 최신 상태로 다시 확인하는지 본다.
// 먼저 온 요청(A)이 메모를 잠근 채 검토를 만들고 있을 때 나중 요청(B)이 들어오는 상황을 만든다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestVectorStoreConfig.class)
@DisplayName("ReviewWriter 동시 저장")
class ReviewWriterLockTest {

    @Autowired private ReviewWriter reviewWriter;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MemoRepository memoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private ReviewFindingRepository findingRepository;
    @Autowired private ReviewMessageRepository messageRepository;

    @Test
    @DisplayName("검토 시작: 먼저 온 요청이 저장하는 동안 기다렸다가, 진행 중 검토를 보고 REVIEW_ALREADY_IN_PROGRESS")
    void concurrentStart_secondWaitsAndSeesActive() throws Exception {
        Memo memo = saveMemo();

        Throwable second = whileHoldingMemoLock(memo.getId(),
                () -> reviewRepository.save(Review.start(memoRepository.getReferenceById(memo.getId()), 2, "A 본문")),
                () -> reviewWriter.saveNewReview(memo.getId(), "B 본문", List.of()));

        assertThat(second).isInstanceOf(ReviewException.class);
        assertThat(((ReviewException) second).getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS);
        assertThat(activeVersions(memo.getId())).containsExactly(2);
    }

    @Test
    @DisplayName("다시 검토: 먼저 온 요청이 v3를 만드는 동안 기다렸다가, v2가 이미 끝난 걸 보고 REVIEW_NOT_FOUND")
    void concurrentRerun_secondWaitsAndSeesEnded() throws Exception {
        Memo memo = saveMemo();
        Long v2 = reviewRepository.save(Review.start(memo, 2, "v2 본문")).getId();

        Throwable second = whileHoldingMemoLock(memo.getId(),
                () -> {
                    reviewRepository.findById(v2).orElseThrow().end();
                    reviewRepository.save(Review.start(memoRepository.getReferenceById(memo.getId()), 3, "A 본문"));
                },
                () -> reviewWriter.saveRerun(memo.getId(), v2, "B 본문", List.of(), 10));

        assertThat(second).isInstanceOf(ReviewException.class);
        assertThat(((ReviewException) second).getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        assertThat(activeVersions(memo.getId())).containsExactly(3);
    }

    // 실제 서버는 요청 하나가 영속성 컨텍스트 하나를 끝까지 쓴다(open-in-view).
    // 그래서 저장 전에 읽어 둔 검토·카드가 캐시에 남아 있어도, 잠근 뒤에는 DB의 최신 상태로 판단해야 한다
    @Test
    @DisplayName("다시 검토: 같은 요청에서 먼저 읽어 둔 검토가 그 사이 다른 요청에 끝났으면 REVIEW_NOT_FOUND")
    void rerun_staleReviewInSameRequest_isRejected() {
        Memo memo = saveMemo();
        Long v3 = reviewRepository.save(Review.start(memo, 3, "v3 본문")).getId();

        Throwable thrown = inOneRequest(() -> {
            reviewWriter.prepareRerun(memo.getUser().getId(), v3);
            otherRequest(() -> {
                reviewRepository.findById(v3).orElseThrow().end();
                reviewRepository.save(Review.start(memoRepository.getReferenceById(memo.getId()), 4, "다른 탭"));
            });
            reviewWriter.saveRerun(memo.getId(), v3, "B 본문", List.of(), 10);
        });

        assertThat(thrown).isInstanceOf(ReviewException.class);
        assertThat(((ReviewException) thrown).getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        assertThat(activeVersions(memo.getId())).containsExactly(4);
    }

    @Test
    @DisplayName("AI 질문 저장: 같은 요청에서 답을 저장한 뒤 다른 요청이 카드를 반영했으면 질문을 저장하지 않는다")
    void saveAiMessage_staleFindingInSameRequest_isSkipped() {
        Memo memo = saveMemo();
        Review review = reviewRepository.save(Review.start(memo, 2, "본문"));
        Long findingId = findingRepository.save(ReviewFinding.create(review, 1, FindingCategory.CRITERIA, "2. 타깃",
                "본문", "제목", "짚은 곳", "근거", "질문")).getId();

        AtomicReference<ReviewMessageResponse> saved = new AtomicReference<>();
        Throwable thrown = inOneRequest(() -> {
            reviewWriter.saveAnswer(memo.getUser().getId(), review.getId(), findingId, "첫 답");
            otherRequest(() -> findingRepository.findById(findingId).orElseThrow().apply("다른 탭에서 반영"));
            saved.set(reviewWriter.saveAiMessage(findingId, "후속 질문?"));
        });

        assertThat(thrown).isNull();
        assertThat(saved.get()).isNull();
        assertThat(messageRepository.countByFindingIdAndRole(findingId, ReviewMessageRole.AI)).isZero();
    }

    // open-in-view처럼, 여러 트랜잭션이 영속성 컨텍스트 하나를 같이 쓰게 한다
    private Throwable inOneRequest(Runnable work) {
        EntityManager shared = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(shared));
        try {
            work.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            shared.close();
        }
    }

    // 다른 탭의 요청: 다른 스레드에서 따로 커밋한다
    private void otherRequest(Runnable work) {
        CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> work.run())).join();
    }

    // A: 메모 잠금 → (B 출발) → 잠시 뒤 firstWork 저장 → 커밋. B가 던진 예외를 돌려준다
    private Throwable whileHoldingMemoLock(Long memoId, Runnable firstWork, Runnable secondRequest) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("SELECT memo_id FROM memo WHERE memo_id = :memoId FOR UPDATE")
                    .setParameter("memoId", memoId)
                    .getResultList();
            locked.countDown();
            sleep(300);
            firstWork.run();
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> second = CompletableFuture.runAsync(secondRequest);
        first.get(10, TimeUnit.SECONDS);
        try {
            second.get(10, TimeUnit.SECONDS);
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            return e.getCause();
        }
    }

    private List<Integer> activeVersions(Long memoId) {
        return transactionTemplate.execute(status -> reviewRepository.findAll().stream()
                .filter(review -> review.getMemo().getId().equals(memoId))
                .filter(review -> review.getStatus() == ReviewStatus.ACTIVE)
                .map(Review::getVersion)
                .toList());
    }

    private Memo saveMemo() {
        User user = userRepository.save(User.createSocialUser(
                "review-lock-" + UUID.randomUUID() + "@test.com", "테스터", null, "google"));
        return memoRepository.save(Memo.createMemo("제목", "본문", user));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
