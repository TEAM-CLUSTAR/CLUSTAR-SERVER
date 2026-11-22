package org.project.domain.memo.repository;

import org.project.domain.memo.entity.MemoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoTagRepository extends JpaRepository<MemoTag, Long> {

    @Modifying
    @Query("DELETE FROM MemoTag mt WHERE mt.memo.id IN :memoIds")
    void deleteByMemoIdIn(@Param("memoIds") List<Long> memoIds);

    @Modifying
    @Query("DELETE FROM MemoTag mt WHERE mt.tag.id IN :tagIds")
    void deleteByTagIds(@Param("tagIds") List<Long> tagIds);
}
