package org.project.domain.label.repository;

import org.project.domain.label.entity.Label;
import org.project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByNameAndUser(String name, User user);
}
