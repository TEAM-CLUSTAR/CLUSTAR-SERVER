package org.project.domain.ai.repository;

import org.project.domain.ai.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByUserIdAndIsDeletedFalse(Long userId);

    /**
     * 활성 채팅방 조회. @Where(is_deleted = false)로 삭제된 방은 제외된다.
     * 삭제와 생성이 한 트랜잭션에서 일어나면 createdAt이 동률로 기록될 수 있어 id를 기준으로 정렬한다.
     */
    Optional<ChatRoom> findTopByUserIdOrderByIdDesc(Long userId);
}

