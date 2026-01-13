package org.project.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.label.entity.Label;
import org.project.domain.user.entity.User;
import org.project.global.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "memo")
public class Memo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "is_ai_generated", nullable = false)
    @Builder.Default
    private Boolean isAiGenerated = false;

    @Column(name = "source")
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("labelPriority ASC")
    @Builder.Default
    private List<MemoLabel> memoLabels = new ArrayList<>();

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imagePriority ASC")
    @Builder.Default
    private List<MemoImage> memoImages = new ArrayList<>();

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("filePriority ASC")
    @Builder.Default
    private List<MemoFile> memoFiles = new ArrayList<>();


    // 일반 메모 생성
    public static Memo createMemo(String title, String content, User user) {
        Memo memo = Memo.builder()
                .title(title)
                .content(content)
                .user(user)
                .build();

        user.getMemos().add(memo);

        return memo;
    }


    public List<Label> getLabels() {
        return memoLabels.stream()
                .map(MemoLabel::getLabel)
                .toList();
    }

    public void addLabel(Label label,Integer labelPriority) {
        MemoLabel memoLabel = MemoLabel.create(this, label, labelPriority);
        this.memoLabels.add(memoLabel);
    }

    public void delete() {
        this.isDeleted = true;
    }

    // 이미지 추가
    public void addImage(String imageS3Key, Long imageBytes, String imageExtension, Integer imagePriority) {
        MemoImage memoImage = MemoImage.builder()
                .memo(this)
                .imageS3Key(imageS3Key)
                .imageBytes(imageBytes)
                .imageExtension(imageExtension)
                .imagePriority(imagePriority)
                .build();

        this.memoImages.add(memoImage);
    }

    // 파일 추가
    public void addFile(String fileS3Key, Long fileBytes, String fileExtension, Integer filePriority) {
        MemoFile memoFile = MemoFile.builder()
                .memo(this)
                .fileS3Key(fileS3Key)
                .fileBytes(fileBytes)
                .fileExtension(fileExtension)
                .filePriority(filePriority)
                .build();

        this.memoFiles.add(memoFile);
    }

}
