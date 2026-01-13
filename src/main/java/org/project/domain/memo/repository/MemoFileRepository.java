package org.project.domain.memo.repository;

import org.project.domain.memo.entity.MemoFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoFileRepository extends JpaRepository<MemoFile, Long> {
}
