package org.project.domain.review.repository;

import org.project.domain.review.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {

    List<ReviewFinding> findByReviewIdOrderByNumberAsc(Long reviewId);

    // 반영할 때 같은 카드가 두 번 반영되지 않도록 행을 잠근다. 없으면 비어 있다.
    // (@Lock은 PostgreSQL 방언에서 FOR NO KEY UPDATE를 만들어 테스트용 H2가 해석하지 못하므로
    //  MemoRepository.lockTrashedIds처럼 네이티브 FOR UPDATE 사용)
    @Query(value = "SELECT review_finding_id FROM review_finding WHERE review_finding_id = :id FOR UPDATE",
            nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);
}
