package org.project.domain.memo.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.project.domain.memo.entity.Memo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo,Long>, MemoRepositoryCustom {
    @Query("SELECT m FROM Memo m WHERE m.id = :memoId AND m.isDeleted = false")
    Optional<Memo> findByIdAndNotDeleted(@Param("memoId") Long memoId);

    @Query("SELECT m FROM Memo m WHERE m.user.id = :userId AND m.isDeleted = false")
    Optional<Memo> findByUserIdAndNotDeleted(@Param("userId") Long userId);
}
