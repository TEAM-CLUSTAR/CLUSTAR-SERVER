package org.project.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.project.domain.memo.entity.Memo;
import org.project.global.entity.BaseEntity;

// 검토 AI 호출 기록(디버깅·품질 확인용). 시도마다 한 줄씩 남긴다.
// 요청 원문에 메모 본문과 사용자 답이 들어가므로, 메모가 영구 삭제되면 DB가 기록도 함께 지운다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_ai_call_log")
public class ReviewAiCallLog extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_ai_call_log_id")
    private Long id;

    @Column(name = "memo_id", nullable = false)
    private Long memoId;

    // FK + ON DELETE CASCADE만 걸기 위한 연결. 저장은 위 memoId로 한다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Memo memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private ReviewAiCallType callType;

    @Column(name = "model")
    private String model;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    // 실패했어도 응답이 왔다면(예: JSON이 깨짐) 원문을 남긴다
    @Column(name = "response_text", columnDefinition = "TEXT")
    private String responseText;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static ReviewAiCallLog success(ReviewAiCall call, String responseText, long latencyMs) {
        ReviewAiCallLog log = from(call);
        log.responseText = responseText;
        log.success = true;
        log.latencyMs = latencyMs;
        return log;
    }

    public static ReviewAiCallLog failure(ReviewAiCall call, String responseText, Exception e) {
        ReviewAiCallLog log = from(call);
        log.responseText = responseText;
        log.success = false;
        log.errorMessage = truncate(e.getMessage());
        return log;
    }

    private static ReviewAiCallLog from(ReviewAiCall call) {
        ReviewAiCallLog log = new ReviewAiCallLog();
        log.memoId = call.memoId();
        log.callType = call.type();
        log.model = call.model();
        log.systemPrompt = call.systemPrompt();
        log.userPrompt = call.userPrompt();
        return log;
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }

    // 한 번의 AI 호출에 무엇을 보냈는지
    public record ReviewAiCall(
            Long memoId,
            ReviewAiCallType type,
            String model,
            String systemPrompt,
            String userPrompt
    ) {
    }
}
