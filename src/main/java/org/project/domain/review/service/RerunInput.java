package org.project.domain.review.service;

import org.project.domain.review.ai.dto.ResolvedGap;

import java.util.List;

// 다시 검토 재료: 지금 본문과, 반영하지 않아 새 버전으로 옮겨 갈 카드의 문장(AI에게 '이미 짚은 문장'으로 알려준다),
// 이 메모에서 지금까지 반영한 빈틈(해결됐으면 같은 빈틈을 다시 내지 않도록 알려준다)
public record RerunInput(
        Long memoId,
        String title,
        String content,
        List<String> excludedAnchors,
        List<ResolvedGap> resolvedGaps
) {
}
