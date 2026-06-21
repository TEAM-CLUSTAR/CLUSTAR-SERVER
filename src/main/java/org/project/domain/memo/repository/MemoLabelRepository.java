package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoLabelRepository extends JpaRepository<MemoLabel, Long> {

    @Modifying
    @Query("DELETE FROM MemoLabel ml WHERE ml.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);

    @Modifying
    @Query("DELETE FROM MemoLabel ml WHERE ml.label.id IN :labelIds")
    void deleteByLabelIds(@Param("labelIds") List<Long> labelIds);
}
