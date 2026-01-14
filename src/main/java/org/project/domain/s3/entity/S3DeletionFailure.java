package org.project.domain.s3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "s3_deletion_failure")
public class S3DeletionFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "s3_deletion_failure_id")
    private Long id;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "memo_id")
    private Long memoId;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType; // "image" or "file"

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "error_message", length = 500) // 에러 메시지 길이 제한
    private String errorMessage;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;
}