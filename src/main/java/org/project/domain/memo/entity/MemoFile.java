package org.project.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "memo_file")
public class MemoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @Column(name = "file_S3_key", nullable = false, length = 500)
    private String fileS3Key;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_bytes")
    private Long fileBytes;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @Column(name = "file_priority", nullable = false)
    private Integer filePriority;
}
