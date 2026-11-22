package org.project.domain.review.repository;

import org.project.domain.review.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {

    List<ReviewFinding> findByReviewIdOrderByNumberAsc(Long reviewId);
}
