package org.project.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.memo.entity.converter.SourceMemoSnapshotsConverter;
import org.project.domain.tag.entity.Tag;
import org.project.domain.user.entity.User;
import org.project.global.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "source")
    private String source;

    // AI 메모가 참고한 원본의 ID·제목 스냅샷 (생성 시점). 원본이 영구 삭제돼도 '사용한 메모'에 제목을 보여주기 위해 저장한다.
    @Convert(converter = SourceMemoSnapshotsConverter.class)
    @Column(name = "source_snapshots", columnDefinition = "TEXT")
    @Builder.Default
    private List<SourceMemoSnapshot> sourceSnapshots = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tagPriority ASC")
    @Builder.Default
    private List<MemoTag> memoTags = new ArrayList<>();

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    // 휴지통으로 옮긴 시각. 보존 기간이 지나면 영구 삭제 대상이 된다.
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 마지막으로 열람(상세조회)한 시각. 한 번도 열지 않은 메모는 null.
    // 검색 모달의 "최근 열람한 메모" 목록 정렬/카드 날짜 표기에 사용된다.
    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

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

    // RAG/AI로 생성된 메모 생성. sources = 참고한 원본(요청 순서)의 ID·제목
    public static Memo createAiMemo(String title, String content, User user, List<SourceMemoSnapshot> sources) {
        Memo memo = Memo.builder()
                .title(title)
                .content(content)
                .user(user)
                .isAiGenerated(true)
                .isNew(true)
                .source(formatSource(sources.stream().map(SourceMemoSnapshot::memoId).toList()))
                .sourceSnapshots(new ArrayList<>(sources))
                .build();

        user.getMemos().add(memo);

        return memo;
    }

    private static String formatSource(List<Long> sourceMemoIds) {
        if (sourceMemoIds == null || sourceMemoIds.isEmpty()) {
            return "[]";
        }

        String joined = sourceMemoIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        return "[" + joined + "]";
    }


    public List<Tag> getTags() {
        return memoTags.stream()
                .map(MemoTag::getTag)
                .toList();
    }

    public void addTag(Tag tag, Integer tagPriority) {
        MemoTag memoTag = MemoTag.create(this, tag, tagPriority);
        this.memoTags.add(memoTag);
    }

    // 제목·본문 수정. 태그/첨부는 Service에서 컬렉션을 재구성한다.
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

}
