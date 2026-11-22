package org.project.domain.memo.repository;

import org.project.domain.memo.entity.MemoImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemoImageRepository extends JpaRepository<MemoImage, Long>, MemoImageRepositoryCustom {

    List<MemoImage> findByMemoIdIn(List<Long> memoIds);

    // 휴지통 메모의 첨부도 복원될 수 있으므로 S3 미참조 정리 대상에서 빠지도록 삭제 여부와 무관하게 모두 반환한다.
    @Query("select mi.imageS3Key from MemoImage mi")
    List<String> findAllImageS3Keys();

    @Modifying
    @Query("DELETE FROM MemoImage mi WHERE mi.memo.id IN :memoIds")
    void deleteByMemoIdIn(@Param("memoIds") List<Long> memoIds);
}
