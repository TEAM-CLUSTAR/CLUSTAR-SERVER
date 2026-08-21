package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoFileRepository extends JpaRepository<MemoFile, Long>, MemoFileRepositoryCustom {

    List<MemoFile> findByMemoIdIn(List<Long> memoIds);

    @Query("select mf.fileS3Key from MemoFile mf join mf.memo m where m.isDeleted = false")
    List<String> findAllFileS3Keys();

    @Modifying
    @Query("DELETE FROM MemoFile mf WHERE mf.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);
}
