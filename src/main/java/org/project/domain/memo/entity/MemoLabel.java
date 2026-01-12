package org.project.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.project.domain.label.entity.Label;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "memo_label",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"memo_id", "label_id"})
        })
public class MemoLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_label_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    private Memo memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @Column(name = "label_priority", nullable = false)
    private Integer labelPriority;

    public static MemoLabel create(Memo memo, Label label, Integer labelPriority) {
        return MemoLabel.builder()
                .memo(memo)
                .label(label)
                .labelPriority(labelPriority)
                .build();
    }
}
