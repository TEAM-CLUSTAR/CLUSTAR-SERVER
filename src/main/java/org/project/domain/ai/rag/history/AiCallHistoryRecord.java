package org.project.domain.ai.rag.history;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.project.domain.ai.rag.F.augment.dto.RagPrompt;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_call_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCallHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_call_history_id")
    private Long id;

    private String conversationId;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(columnDefinition = "TEXT")
    private String userPrompt;

    @Column(columnDefinition = "TEXT")
    private String fullPrompt;

    @Column(columnDefinition = "TEXT")
    private String responseText;

    private boolean success;

    private Long latencyMs;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt;

    /* 정적 팩토리 메서드 */
    public static AiCallHistoryRecord success(
            RagPrompt prompt,
            String fullPrompt,
            String responseText,
            long latencyMs
    ) {
        AiCallHistoryRecord r = new AiCallHistoryRecord();
        r.conversationId = prompt.conversationId();
        r.systemPrompt = prompt.systemPrompt();
        r.context = prompt.context();
        r.userPrompt = prompt.userPrompt();
        r.fullPrompt = fullPrompt;
        r.responseText = responseText;
        r.success = true;
        r.latencyMs = latencyMs;
        r.createdAt = LocalDateTime.now();
        return r;
    }

    public static AiCallHistoryRecord failure(
            RagPrompt prompt,
            String fullPrompt,
            Exception e
    ) {
        AiCallHistoryRecord r = new AiCallHistoryRecord();
        r.conversationId = prompt.conversationId();
        r.systemPrompt = prompt.systemPrompt();
        r.context = prompt.context();
        r.userPrompt = prompt.userPrompt();
        r.fullPrompt = fullPrompt;
        r.success = false;
        r.errorMessage = safeMessage(e);
        r.createdAt = LocalDateTime.now();
        return r;
    }

    private static String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null) return null;
        return e.getMessage().length() > 2000
                ? e.getMessage().substring(0, 2000)
                : e.getMessage();
    }
}
