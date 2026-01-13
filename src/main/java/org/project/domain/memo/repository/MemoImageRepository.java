package org.project.domain.memo.repository;

import org.springframework.data.repository.query.Param;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemoImageRepository extends JpaRepository<MemoImage, Long> {

    List<MemoImage> findByMemoIdIn(List<Long> memoIds);
    @Modifying
    @Query("DELETE FROM MemoImage mi WHERE mi.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);
}
