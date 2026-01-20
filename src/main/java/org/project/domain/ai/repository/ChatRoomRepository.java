package org.project.domain.ai.repository;

import org.project.domain.ai.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByUserIdAndIsDeletedFalse(Long userId);

    Optional<ChatRoom> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}

