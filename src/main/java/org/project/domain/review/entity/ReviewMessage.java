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

// 카드 안의 대화 한 줄(사용자 답 / AI 후속 질문).
// 첫 질문은 ReviewFinding.question에, 고정 안내 문구는 프론트가 그리므로 여기 저장하지 않는다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_message")
public class ReviewMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_finding_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ReviewFinding finding;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ReviewMessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    public static ReviewMessage of(ReviewFinding finding, ReviewMessageRole role, String content) {
        ReviewMessage message = new ReviewMessage();
        message.finding = finding;
        message.role = role;
        message.content = content;
        return message;
    }
}
