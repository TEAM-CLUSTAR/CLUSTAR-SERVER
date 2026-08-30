package org.project.domain.ai.repository;

import org.project.domain.ai.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 대화 순서대로 조회한다.
     * <p>
     * USER/ASSISTANT 두 건을 한 트랜잭션에서 저장하면 createdAt이 동일한 값으로 기록될 수 있어
     * 삽입 순서가 보장되는 id를 정렬 기준으로 사용한다.
     */
    List<ChatMessage> findByChatRoomIdOrderByIdAsc(Long chatRoomId);
}
