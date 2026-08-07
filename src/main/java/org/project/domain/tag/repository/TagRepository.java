package org.project.domain.tag.repository;

import org.project.domain.tag.entity.Tag;
import org.project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByNameAndUser(String name, User user);

    List<Tag> findAllByNameInAndUser(List<String> names, User user);

    Optional<Tag> findByNameAndUserId(String name, Long userId);

    List<Tag> findAllByUserId(Long userId);

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    List<Tag> findTop10ByUserIdAndParentIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Tag> findByIdAndUserIdAndParentIsNull(Long id, Long userId);

    List<Tag> findByUserIdAndParentIdOrderByCreatedAtDesc(Long userId, Long parentId);

    List<Tag> findByUserIdAndParentParentIdOrderByCreatedAtDesc(Long userId, Long parentId);
}
