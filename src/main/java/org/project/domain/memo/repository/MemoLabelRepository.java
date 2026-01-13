package org.project.domain.memo.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MemoLabelRepository extends JpaRepository<MemoLabel, Long> {

    @Modifying
    @Query("DELETE FROM MemoLabel ml WHERE ml.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);
}
