package org.project.domain.review.ai.dto;

// 다시 검토할 때 AI에게 알려 줄 '이미 반영한 빈틈': 무엇이 빠졌다고 했고(title), 어디를 짚었고(anchorText), 무엇을 넣었는지(appliedText)
public record ResolvedGap(
        String title,
        String anchorText,
        String appliedText
) {
}
