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

    // 메모 수정 시 유지되는 파일의 정렬 우선순위 갱신
    public void updatePriority(Integer filePriority) {
        this.filePriority = filePriority;
    }
}
