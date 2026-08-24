package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
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

    /**
     * 상세조회(열람) 시 열람 시각과 읽음 여부만 직접 UPDATE한다.
     * 엔티티 dirty checking을 타지 않아 {@code @LastModifiedDate}(updatedAt)가 발동하지 않는다.
     * → "열람"이 "수정 시각(updatedAt)"을 오염시키지 않도록 분리한 것.
     */
    @Modifying
    @Query("UPDATE Memo m SET m.lastViewedAt = :viewedAt, m.isNew = false WHERE m.id = :memoId")
    void touchViewed(@Param("memoId") Long memoId, @Param("viewedAt") LocalDateTime viewedAt);

    @Query("SELECT m FROM Memo m JOIN FETCH m.user WHERE m.id = :memoId AND m.isDeleted = false")
    Optional<Memo> findByIdWithUserAndNotDeleted(@Param("memoId") Long memoId);

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
            SELECT DISTINCT m
            FROM Memo m
            LEFT JOIN FETCH m.memoTags mt
            LEFT JOIN FETCH mt.tag
            WHERE m.user.id = :userId
              AND m.isDeleted = false
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Memo> findAllByUserIdWithTagsAndNotDeleted(@Param("userId") Long userId);

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

    @Query("SELECT m.id FROM Memo m WHERE m.isDeleted = false")
    List<Long> findAllNotDeletedMemoIds();
}
