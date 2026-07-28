package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoTagRepository extends JpaRepository<MemoTag, Long> {

    @Modifying
    @Query("DELETE FROM MemoTag mt WHERE mt.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);

    @Modifying
    @Query("DELETE FROM MemoTag mt WHERE mt.tag.id IN :tagIds")
    void deleteByTagIds(@Param("tagIds") List<Long> tagIds);
}
