package org.project.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.project.domain.memo.entity.Memo;
import org.project.global.entity.BaseEntity;

// 검토 한 번 = 버전 하나. 종료하거나 다시 검토해도 지우지 않고 기록으로 남긴다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "review",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_memo_version", columnNames = {"memo_id", "version"})
)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    // 메모가 영구 삭제되면 DB가 검토 기록도 함께 지운다(기존 영구 삭제 코드는 수정하지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Memo memo;

    // 원본 문서가 v1, 검토할 때마다 +1
    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReviewStatus status;

    // 이 버전을 분석한 시점의 본문. 반영하면 본문이 바뀌므로 지금 남겨야 나중에 버전 기록을 보여줄 수 있다
    @Column(name = "content_snapshot", nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    public static Review start(Memo memo, int version, String contentSnapshot) {
        Review review = new Review();
        review.memo = memo;
        review.version = version;
        review.status = ReviewStatus.ACTIVE;
        review.contentSnapshot = contentSnapshot;
        return review;
    }

    public boolean isActive() {
        return this.status == ReviewStatus.ACTIVE;
    }
}
