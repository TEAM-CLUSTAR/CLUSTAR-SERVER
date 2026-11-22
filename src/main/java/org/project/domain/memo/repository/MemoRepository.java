package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo,Long>, MemoRepositoryCustom {
    @Query("SELECT m FROM Memo m WHERE m.id = :memoId AND m.isDeleted = false")
    Optional<Memo> findByIdAndNotDeleted(@Param("memoId") Long memoId);

    // 열람 시각·읽음 여부만 직접 UPDATE. dirty checking을 안 타서 updatedAt(@LastModifiedDate)이 열람으로 오염되지 않는다.
    @Modifying
    @Query("UPDATE Memo m SET m.lastViewedAt = :viewedAt, m.isNew = false WHERE m.id = :memoId")
    void touchViewed(@Param("memoId") Long memoId, @Param("viewedAt") LocalDateTime viewedAt);

    // 임베딩 전용 조회. 휴지통 메모도 복원에 대비해 벡터를 유지하므로 삭제 여부와 무관하게 찾는다(영구 삭제된 메모만 없음).
    @Query("SELECT m FROM Memo m JOIN FETCH m.user WHERE m.id = :memoId")
    Optional<Memo> findByIdWithUser(@Param("memoId") Long memoId);

    @Query("""
            SELECT DISTINCT m
            FROM Memo m
            LEFT JOIN FETCH m.memoTags mt
            LEFT JOIN FETCH mt.tag
            WHERE m.user.id = :userId
              AND m.id IN :memoIds
              AND m.isDeleted = false
            """)
    List<Memo> findByIdInWithTagsAndNotDeleted(
            @Param("userId") Long userId,
            @Param("memoIds") List<Long> memoIds
    );

    @Query("""
        SELECT COUNT(DISTINCT m)
        FROM Memo m
        LEFT JOIN m.memoTags mt
        WHERE m.user.id = :userId
        AND m.isDeleted = false
        AND mt.tag.id IN :tagIds
        """)
    long countMemosByTags(
            @Param("userId") Long userId,
            @Param("tagIds") List<Long> tagIds
    );

    @Query("""
        SELECT COUNT(m)
        FROM Memo m
        WHERE m.user.id = :userId
        AND m.isDeleted = false
        """)
    long countAllMemos(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(m)
        FROM Memo m
        WHERE m.user.id = :userId
        AND m.id IN :memoIds
        AND m.isDeleted = false
        """)
    long countByIdInAndUserIdAndNotDeleted(
            @Param("userId") Long userId,
            @Param("memoIds") List<Long> memoIds
    );

    // 재임베딩 대상. 휴지통 메모도 복원될 수 있어 함께 재임베딩한다.
    @Query("SELECT m.id FROM Memo m")
    List<Long> findAllMemoIds();

    // == 휴지통 == //

    // 휴지통 이동/복원은 직접 UPDATE. dirty checking을 안 타서 updatedAt(최종 수정)이 바뀌지 않는다.
    // 조건에 현재 상태를 넣어 동시 요청이 와도 한 번만 반영된다(반환값 = 반영된 행 수).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Memo m SET m.isDeleted = true, m.deletedAt = :deletedAt WHERE m.id = :memoId AND m.isDeleted = false")
    int moveToTrash(@Param("memoId") Long memoId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Memo m SET m.isDeleted = false, m.deletedAt = null WHERE m.id = :memoId AND m.isDeleted = true")
    int restoreFromTrash(@Param("memoId") Long memoId);

    @Query("SELECT m FROM Memo m WHERE m.id = :memoId AND m.isDeleted = true")
    Optional<Memo> findTrashedById(@Param("memoId") Long memoId);

    @Query("SELECT COUNT(m) FROM Memo m WHERE m.user.id = :userId AND m.isDeleted = true")
    long countTrashedMemos(@Param("userId") Long userId);

    @Query("SELECT m.id FROM Memo m WHERE m.user.id = :userId AND m.isDeleted = true")
    List<Long> findTrashedIdsByUserId(@Param("userId") Long userId);

    // 선택한 메모 중 내 휴지통에 있는 메모 수 (쓸 수 없는 이유가 휴지통인지 구분용)
    @Query("SELECT COUNT(m) FROM Memo m WHERE m.user.id = :userId AND m.id IN :memoIds AND m.isDeleted = true")
    long countTrashedByIdInAndUserId(@Param("userId") Long userId, @Param("memoIds") List<Long> memoIds);

    // 보존 기간이 지난 휴지통 메모 id (자동 영구 삭제용, 배치 크기만큼씩)
    @Query("SELECT m.id FROM Memo m WHERE m.isDeleted = true AND m.deletedAt < :cutoff ORDER BY m.id")
    List<Long> findExpiredTrashedIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    // 영구 삭제 직전에 대상 행을 잠근다. 같은 메모의 복원과 동시에 실행돼도
    // "복원됐는데 태그/첨부만 지워진" 상태가 생기지 않도록, 잠근 휴지통 메모만 삭제 대상으로 삼는다.
    // (@Lock은 PostgreSQL 방언에서 FOR NO KEY UPDATE를 만들어 테스트용 H2가 해석하지 못하므로 네이티브 FOR UPDATE 사용)
    @Query(value = "SELECT memo_id FROM memo WHERE memo_id IN (:memoIds) AND is_deleted = true FOR UPDATE",
            nativeQuery = true)
    List<Long> lockTrashedIds(@Param("memoIds") List<Long> memoIds);

    // 영구 삭제. 휴지통에 있는 메모만 지운다(연관 태그/첨부 행은 먼저 지워야 함).
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Memo m WHERE m.id IN :memoIds AND m.isDeleted = true")
    int deleteTrashedByIdIn(@Param("memoIds") List<Long> memoIds);
}
