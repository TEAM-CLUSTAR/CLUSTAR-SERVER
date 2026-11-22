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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.project.global.entity.BaseEntity;

// 빈틈 카드 하나. 짚은 곳·근거·첫 질문은 검토할 때 AI가 한 번에 만들어 둔다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_finding")
public class ReviewFinding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_finding_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Review review;

    // 본문 마커와 카드를 잇는 번호. 버전 안에서 본문 순서대로 매긴다
    @Column(name = "finding_number", nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private FindingCategory category;

    // 위치. 예: "2. 타깃"
    @Column(name = "section_label")
    private String sectionLabel;

    // AI가 짚은 본문 문장 원문. 프론트는 이 문장으로 밑줄을 긋고, 서버는 반영 위치를 찾는다
    @Column(name = "anchor_text", nullable = false, columnDefinition = "TEXT")
    private String anchorText;

    @Column(name = "title", nullable = false)
    private String title;

    // 짚은 곳
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // 왜 빈틈으로 봤나요
    @Column(name = "evidence", nullable = false, columnDefinition = "TEXT")
    private String evidence;

    // AI 첫 질문
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FindingStatus status;

    // 본문에 넣은 문장. 반영 전에는 null
    @Column(name = "applied_text", columnDefinition = "TEXT")
    private String appliedText;

    public static ReviewFinding create(
            Review review,
            int number,
            FindingCategory category,
            String sectionLabel,
            String anchorText,
            String title,
            String reason,
            String evidence,
            String question
    ) {
        ReviewFinding finding = new ReviewFinding();
        finding.review = review;
        finding.number = number;
        finding.category = category;
        finding.sectionLabel = sectionLabel;
        finding.anchorText = anchorText;
        finding.title = title;
        finding.reason = reason;
        finding.evidence = evidence;
        finding.question = question;
        finding.status = FindingStatus.OPEN;
        return finding;
    }

    // 다시 검토할 때 반영하지 않은 카드를 새 버전으로 옮긴다(이전 버전의 카드는 기록으로 그대로 둔다)
    public ReviewFinding copyTo(Review nextReview, int nextNumber) {
        ReviewFinding copy = create(nextReview, nextNumber, category, sectionLabel, anchorText,
                title, reason, evidence, question);
        copy.status = this.status;
        return copy;
    }

    // 답을 하나라도 보내면 '답하는 중'
    public void markAnswering() {
        this.status = FindingStatus.ANSWERING;
    }

    // 본문에 넣은 문장을 기억해 둔다(검토 중 반영 블록 표시, 다시 검토 시 제외 기준)
    public void apply(String appliedText) {
        this.status = FindingStatus.APPLIED;
        this.appliedText = appliedText;
    }

    public boolean isApplied() {
        return this.status == FindingStatus.APPLIED;
    }
}
