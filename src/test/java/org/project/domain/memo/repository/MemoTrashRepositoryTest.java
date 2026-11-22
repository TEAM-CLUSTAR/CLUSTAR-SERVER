package org.project.domain.memo.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.project.domain.memo.entity.MemoImage;
import org.project.domain.memo.entity.SourceMemoSnapshot;
import org.project.domain.user.entity.User;
import org.project.domain.user.repository.UserRepository;
import org.project.global.config.querydsl.QuerydslTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslTestConfig.class)
@DisplayName("휴지통 Repository 테스트")
class MemoTrashRepositoryTest {

    @Autowired MemoRepository memoRepository;
    @Autowired MemoImageRepository memoImageRepository;
    @Autowired MemoFileRepository memoFileRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    private User user;
    private User otherUser;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 12, 0);

    @BeforeEach
    void setUp() {
        user = userRepository.save(newUser());
        otherUser = userRepository.save(newUser());
    }

    private User newUser() {
        return User.createSocialUser("u" + UUID.randomUUID() + "@test.com", "유저", "p.png", "google");
    }

    private Memo saveMemo(User owner, String title) {
        return memoRepository.save(Memo.createMemo(title, "내용", owner));
    }

    // 테스트에서는 auditing이 꺼져 있어 직접 넣는다
    private Memo saveTrashed(User owner, String title, LocalDateTime deletedAt) {
        Memo memo = saveMemo(owner, title);
        ReflectionTestUtils.setField(memo, "isDeleted", true);
        ReflectionTestUtils.setField(memo, "deletedAt", deletedAt);
        return memo;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("moveToTrash는 isDeleted/deletedAt만 바꾸고 updatedAt은 그대로 둔다. 이미 휴지통이면 0건")
    void moveToTrash_keepsUpdatedAt() {
        Memo memo = saveMemo(user, "메모");
        ReflectionTestUtils.setField(memo, "updatedAt", BASE);
        flushAndClear();

        int first = memoRepository.moveToTrash(memo.getId(), BASE.plusDays(1));
        int second = memoRepository.moveToTrash(memo.getId(), BASE.plusDays(2));

        Memo found = memoRepository.findById(memo.getId()).orElseThrow();
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(found.getIsDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isEqualTo(BASE.plusDays(1));
        assertThat(found.getUpdatedAt()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("restoreFromTrash는 휴지통 상태를 되돌리고, 휴지통에 없으면 0건")
    void restoreFromTrash() {
        Memo trashed = saveTrashed(user, "휴지통", BASE);
        Memo alive = saveMemo(user, "살아있음");
        flushAndClear();

        assertThat(memoRepository.restoreFromTrash(trashed.getId())).isEqualTo(1);
        assertThat(memoRepository.restoreFromTrash(alive.getId())).isZero();

        Memo restored = memoRepository.findById(trashed.getId()).orElseThrow();
        assertThat(restored.getIsDeleted()).isFalse();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("findTrashedById는 휴지통 메모만 찾는다")
    void findTrashedById() {
        Memo trashed = saveTrashed(user, "휴지통", BASE);
        Memo alive = saveMemo(user, "살아있음");
        flushAndClear();

        assertThat(memoRepository.findTrashedById(trashed.getId())).isPresent();
        assertThat(memoRepository.findTrashedById(alive.getId())).isEmpty();
    }

    @Test
    @DisplayName("휴지통 목록은 내 휴지통 메모만 deletedAt 최신순으로, 커서 다음부터 조회한다")
    void findTrashedMemos_orderAndCursor() {
        Memo oldest = saveTrashed(user, "oldest", BASE);
        Memo middle = saveTrashed(user, "middle", BASE.plusHours(1));
        Memo newest = saveTrashed(user, "newest", BASE.plusHours(2));
        saveMemo(user, "살아있음");
        saveTrashed(otherUser, "남의 휴지통", BASE.plusHours(3));
        flushAndClear();

        List<Memo> firstPage = memoRepository.findTrashedMemos(user.getId(), null, null, 2);
        Memo last = firstPage.get(firstPage.size() - 1);
        List<Memo> secondPage = memoRepository.findTrashedMemos(user.getId(), last.getDeletedAt(), last.getId(), 2);

        assertThat(firstPage).extracting(Memo::getId).containsExactly(newest.getId(), middle.getId());
        assertThat(secondPage).extracting(Memo::getId).containsExactly(oldest.getId());
        assertThat(memoRepository.countTrashedMemos(user.getId())).isEqualTo(3);
        assertThat(memoRepository.findTrashedIdsByUserId(user.getId()))
                .containsExactlyInAnyOrder(oldest.getId(), middle.getId(), newest.getId());
    }

    @Test
    @DisplayName("deletedAt이 같으면 memoId 내림차순으로 이어서 조회한다")
    void findTrashedMemos_sameDeletedAt() {
        Memo a = saveTrashed(user, "a", BASE);
        Memo b = saveTrashed(user, "b", BASE);
        flushAndClear();

        List<Memo> firstPage = memoRepository.findTrashedMemos(user.getId(), null, null, 1);
        List<Memo> secondPage = memoRepository.findTrashedMemos(user.getId(), BASE, firstPage.get(0).getId(), 1);

        assertThat(firstPage).extracting(Memo::getId).containsExactly(b.getId());
        assertThat(secondPage).extracting(Memo::getId).containsExactly(a.getId());
    }

    @Test
    @DisplayName("만료 조회는 cutoff 이전에 휴지통으로 옮긴 메모만, afterId 다음부터 배치 크기만큼 반환한다")
    void findExpiredTrashedIds() {
        Memo expired1 = saveTrashed(user, "만료1", BASE.minusDays(40));
        Memo expired2 = saveTrashed(otherUser, "만료2", BASE.minusDays(31));
        saveTrashed(user, "보존", BASE.minusDays(10));
        saveMemo(user, "살아있음");
        flushAndClear();

        LocalDateTime cutoff = BASE.minusDays(30);

        assertThat(memoRepository.findExpiredTrashedIds(cutoff, 0L, PageRequest.of(0, 10)))
                .containsExactly(expired1.getId(), expired2.getId());
        assertThat(memoRepository.findExpiredTrashedIds(cutoff, 0L, PageRequest.of(0, 1)))
                .containsExactly(expired1.getId());
        // ID 커서: 앞 배치의 마지막 ID 다음부터
        assertThat(memoRepository.findExpiredTrashedIds(cutoff, expired1.getId(), PageRequest.of(0, 1)))
                .containsExactly(expired2.getId());
    }

    @Test
    @DisplayName("자동 삭제용 잠금은 보존 기간이 지난 휴지통 메모만 잡는다 (복원 후 다시 휴지통에 넣은 메모 제외)")
    void lockExpiredTrashedIds() {
        Memo expired = saveTrashed(user, "만료", BASE.minusDays(40));
        Memo reTrashed = saveTrashed(user, "조회 후 복원했다가 다시 삭제", BASE);
        Memo alive = saveMemo(user, "살아있음");
        flushAndClear();

        List<Long> ids = List.of(expired.getId(), reTrashed.getId(), alive.getId());

        assertThat(memoRepository.lockExpiredTrashedIds(ids, BASE.minusDays(30)))
                .containsExactly(expired.getId());
    }

    @Test
    @DisplayName("수정 저장은 바뀐 컬럼만 UPDATE해서, 그 사이 커밋된 휴지통 이동을 되돌리지 않는다")
    void editAfterConcurrentTrash_keepsTrashState() {
        Memo memo = saveMemo(user, "원래 제목");
        flushAndClear();

        // 편집 요청이 메모를 읽어 둔 상태
        Memo loaded = memoRepository.findById(memo.getId()).orElseThrow();
        // 그 사이 다른 요청이 휴지통으로 이동 (영속성 컨텍스트를 건드리지 않는 직접 UPDATE)
        em.getEntityManager().createNativeQuery(
                        "UPDATE memo SET is_deleted = true, deleted_at = :at WHERE memo_id = :id")
                .setParameter("at", BASE)
                .setParameter("id", memo.getId())
                .executeUpdate();
        // 편집 저장
        loaded.update("새 제목", "새 본문");
        flushAndClear();

        Memo found = memoRepository.findById(memo.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("새 제목");
        assertThat(found.getIsDeleted()).isTrue();
        assertThat(found.getDeletedAt()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("잠금·영구 삭제는 휴지통 메모에만 적용된다")
    void lockAndDeleteOnlyTrashed() {
        Memo trashed = saveTrashed(user, "휴지통", BASE);
        Memo alive = saveMemo(user, "살아있음");
        flushAndClear();

        List<Long> ids = List.of(trashed.getId(), alive.getId());

        assertThat(memoRepository.lockTrashedIds(ids)).containsExactly(trashed.getId());
        assertThat(memoRepository.deleteTrashedByIdIn(ids)).isEqualTo(1);
        assertThat(memoRepository.findById(trashed.getId())).isEmpty();
        assertThat(memoRepository.findById(alive.getId())).isPresent();
    }

    @Test
    @DisplayName("선택한 메모 중 내 휴지통 메모만 센다 (정상 메모·남의 휴지통 메모 제외)")
    void countTrashedByIdInAndUserId() {
        Memo trashed = saveTrashed(user, "내 휴지통", BASE);
        Memo alive = saveMemo(user, "살아있음");
        Memo othersTrashed = saveTrashed(otherUser, "남의 휴지통", BASE);
        flushAndClear();

        long count = memoRepository.countTrashedByIdInAndUserId(
                user.getId(), List.of(trashed.getId(), alive.getId(), othersTrashed.getId()));

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("AI 메모의 원본 스냅샷(ID·제목)이 JSON으로 저장되고 그대로 다시 읽힌다")
    void sourceSnapshots_roundTrip() {
        Memo aiMemo = memoRepository.save(Memo.createAiMemo("AI 메모", "본문", user, List.of(
                new SourceMemoSnapshot(3L, "원본 \"따옴표\", 쉼표"),
                new SourceMemoSnapshot(1L, "원본1"))));
        flushAndClear();

        Memo found = memoRepository.findById(aiMemo.getId()).orElseThrow();

        assertThat(found.getSourceSnapshots()).containsExactly(
                new SourceMemoSnapshot(3L, "원본 \"따옴표\", 쉼표"),
                new SourceMemoSnapshot(1L, "원본1"));
        assertThat(found.getSource()).isEqualTo("[3, 1]");
    }

    @Test
    @DisplayName("S3 미참조 정리용 키 목록에 휴지통 메모의 첨부도 포함된다 (복원 전 S3 삭제 방지)")
    void allS3Keys_includeTrashedMemoAttachments() {
        Memo trashed = saveTrashed(user, "휴지통", BASE);
        memoImageRepository.save(MemoImage.builder()
                .memo(trashed).imageS3Key("memo-image/1/trashed.png").imagePriority(0).build());
        memoFileRepository.save(MemoFile.builder()
                .memo(trashed).fileS3Key("memo-file/1/trashed.pdf").filePriority(0).build());
        flushAndClear();

        assertThat(memoImageRepository.findAllImageS3Keys()).contains("memo-image/1/trashed.png");
        assertThat(memoFileRepository.findAllFileS3Keys()).contains("memo-file/1/trashed.pdf");
    }
}
