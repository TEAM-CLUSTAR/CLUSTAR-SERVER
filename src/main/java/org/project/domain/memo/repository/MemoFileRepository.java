package org.project.domain.memo.repository;

import org.project.domain.memo.entity.MemoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoFileRepository extends JpaRepository<MemoFile, Long>, MemoFileRepositoryCustom {

    List<MemoFile> findByMemoIdIn(List<Long> memoIds);

    // 휴지통 메모의 첨부도 복원될 수 있으므로 S3 미참조 정리 대상에서 빠지도록 삭제 여부와 무관하게 모두 반환한다.
    @Query("select mf.fileS3Key from MemoFile mf")
    List<String> findAllFileS3Keys();

    @Modifying
    @Query("DELETE FROM MemoFile mf WHERE mf.memo.id IN :memoIds")
    void deleteByMemoIdIn(@Param("memoIds") List<Long> memoIds);
}
