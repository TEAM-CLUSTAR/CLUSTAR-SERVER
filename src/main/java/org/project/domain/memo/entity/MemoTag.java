package org.project.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.tag.entity.Tag;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "memo_tag",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"memo_id", "tag_id"})
        })
public class MemoTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "tag_priority", nullable = false)
    private Integer tagPriority;

    public static MemoTag create(Memo memo, Tag tag, Integer tagPriority) {
        return MemoTag.builder()
                .memo(memo)
                .tag(tag)
                .tagPriority(tagPriority)
                .build();
    }

    public void updatePriority(Integer tagPriority) {
        this.tagPriority = tagPriority;
    }
}
