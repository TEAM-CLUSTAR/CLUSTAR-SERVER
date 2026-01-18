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
}
