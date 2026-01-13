package org.project.domain.memo.repository;

import org.project.domain.memo.entity.MemoImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoImageRepository extends JpaRepository<MemoImage, Long> {
}
