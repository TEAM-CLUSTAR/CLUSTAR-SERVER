package org.project.domain.memo.repository;

import org.project.domain.memo.entity.MemoImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoImageRepository extends JpaRepository<MemoImage, Long> {

    List<MemoImage> findByMemoIdIn(List<Long> memoIds);
}
