package org.project.domain.s3.repository;

import org.project.domain.s3.entity.S3DeletionFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3DeletionFailureRepository extends JpaRepository<S3DeletionFailure, Long> {
}
