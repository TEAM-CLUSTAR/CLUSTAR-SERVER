package org.project.domain.review.service;

import java.util.List;

// 다시 검토 재료: 지금 본문과, 반영하지 않아 새 버전으로 옮겨 갈 카드의 문장(AI에게 '이미 짚은 문장'으로 알려준다)
public record RerunInput(
        Long memoId,
        String title,
        String content,
        List<String> excludedAnchors
) {
}
