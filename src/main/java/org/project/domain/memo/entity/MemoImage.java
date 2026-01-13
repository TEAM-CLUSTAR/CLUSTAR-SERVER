package org.project.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "memo_image")
public class MemoImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @Column(name = "image_S3_key", nullable = false, length = 500)
    private String imageS3Key;

    @Column(name = "image_bytes")
    private Long imageBytes;

    @Column(name = "image_extension", length = 10)
    private String imageExtension;

    @Column(name = "image_priority", nullable = false)
    private Integer imagePriority;
}

