package org.project.domain.memo.repository;

import org.springframework.data.repository.query.Param;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemoFileRepository extends JpaRepository<MemoFile, Long> {

    List<MemoFile> findByMemoIdIn(List<Long> memoIds);
    @Modifying
    @Query("DELETE FROM MemoFile mf WHERE mf.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);
}
