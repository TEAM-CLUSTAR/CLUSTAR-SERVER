package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo,Long>, MemoRepositoryCustom {
    @Query("SELECT m FROM Memo m WHERE m.id = :memoId AND m.isDeleted = false")
    Optional<Memo> findByIdAndNotDeleted(@Param("memoId") Long memoId);

    @Query("SELECT m FROM Memo m JOIN FETCH m.user WHERE m.id = :memoId AND m.isDeleted = false")
    Optional<Memo> findByIdWithUserAndNotDeleted(@Param("memoId") Long memoId);

    @Query("""
            SELECT DISTINCT m
            FROM Memo m
            LEFT JOIN FETCH m.memoLabels ml
            LEFT JOIN FETCH ml.label
            WHERE m.user.id = :userId
              AND m.id IN :memoIds
              AND m.isDeleted = false
            """)
    List<Memo> findByIdInWithLabelsAndNotDeleted(
            @Param("userId") Long userId,
            @Param("memoIds") List<Long> memoIds
    );

    @Query("""
            SELECT DISTINCT m
            FROM Memo m
            LEFT JOIN FETCH m.memoLabels ml
            LEFT JOIN FETCH ml.label
            WHERE m.user.id = :userId
              AND m.isDeleted = false
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    List<Memo> findAllByUserIdWithLabelsAndNotDeleted(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(DISTINCT m)
        FROM Memo m
        LEFT JOIN m.memoLabels ml
        WHERE m.user.id = :userId
        AND m.isDeleted = false
        AND (:labelIds IS NULL OR ml.label.id IN :labelIds)
        """)
    long countMemos(
            @Param("userId") Long userId,
            @Param("labelIds") List<Long> labelIds
    );
}
