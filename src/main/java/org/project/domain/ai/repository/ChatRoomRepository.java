package org.project.domain.ai.repository;

import org.project.domain.ai.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByUserIdAndIsDeletedFalse(Long userId);
}

