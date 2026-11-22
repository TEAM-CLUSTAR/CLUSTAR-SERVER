package org.project.domain.review.repository;

import org.project.domain.review.entity.ReviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReviewMessageRepository extends JpaRepository<ReviewMessage, Long> {

    // 카드 여러 장의 대화를 한 번에 조회한다(카드마다 조회하는 N+1 방지).
    // 답과 후속 질문을 한 트랜잭션에서 저장하면 createdAt이 같을 수 있어 id로 정렬한다
    List<ReviewMessage> findByFindingIdInOrderByIdAsc(Collection<Long> findingIds);
}
