package org.project.domain.review.repository;

import org.project.domain.review.entity.Review;
import org.project.domain.review.entity.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByMemoIdAndStatus(Long memoId, ReviewStatus status);

    // 이 메모의 마지막 검토 버전(종료 여부 무관). 검토한 적이 없으면 비어 있다
    @Query("SELECT MAX(r.version) FROM Review r WHERE r.memo.id = :memoId")
    Optional<Integer> findLatestVersion(@Param("memoId") Long memoId);
}
