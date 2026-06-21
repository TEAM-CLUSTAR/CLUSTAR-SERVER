package org.project.domain.label.repository;

import org.project.domain.label.entity.Label;
import org.project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByNameAndUser(String name, User user);

    Optional<Label> findByNameAndUserId(String name, Long userId);

    List<Label> findAllByUserId(Long userId);

    Optional<Label> findByIdAndUserId(Long id, Long userId);

    List<Label> findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Label> findByIdAndUserIdAndParentIsNull(Long id, Long userId);

    List<Label> findByUserIdAndParentIdOrderByCreatedAtDesc(Long userId, Long parentId);

    List<Label> findByUserIdAndParentParentIdOrderByCreatedAtDesc(Long userId, Long parentId);
}
