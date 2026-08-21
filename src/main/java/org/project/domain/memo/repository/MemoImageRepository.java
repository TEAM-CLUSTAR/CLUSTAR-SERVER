package org.project.domain.memo.repository;

import org.project.domain.memo.entity.Memo;
import org.project.domain.memo.entity.MemoImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoImageRepository extends JpaRepository<MemoImage, Long>, MemoImageRepositoryCustom {

    List<MemoImage> findByMemoIdIn(List<Long> memoIds);

    @Query("select mi.imageS3Key from MemoImage mi join mi.memo m where m.isDeleted = false")
    List<String> findAllImageS3Keys();

    @Modifying
    @Query("DELETE FROM MemoImage mi WHERE mi.memo = :memo")
    void deleteByMemo(@Param("memo") Memo memo);
}
