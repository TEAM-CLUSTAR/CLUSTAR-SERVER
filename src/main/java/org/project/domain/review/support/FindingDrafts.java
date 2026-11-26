package org.project.domain.review.support;

import org.project.domain.review.ai.dto.FindingDraft;
import org.project.domain.review.entity.FindingCategory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// AI가 준 빈틈 목록을 저장할 수 있는 카드로 거른다.
// 1) 유형이 이상하거나 빈 항목이 있으면 버림  2) 본문에서 문장을 못 찾으면 버림
// 3) 같은 문장 중복 제거  4) AI 순서(중요한 순)로 최대 개수만 남김  5) 본문 순서로 정렬
public final class FindingDrafts {

    private FindingDrafts() {
    }

    public static List<ValidFindingDraft> sanitize(String content, List<FindingDraft> drafts, int maxFindings) {
        Set<String> seenAnchors = new HashSet<>();

        return drafts.stream()
                .map(draft -> validate(content, draft))
                .flatMap(Optional::stream)
                .filter(located -> seenAnchors.add(located.draft().anchorText()))
                .limit(maxFindings)
                .sorted(Comparator.comparingInt(Positioned::start))
                .map(Positioned::draft)
                .toList();
    }

    private static Optional<Positioned> validate(String content, FindingDraft draft) {
        Optional<FindingCategory> category = parseCategory(draft.category());
        if (category.isEmpty() || hasBlank(draft)) {
            return Optional.empty();
        }

        return AnchorTextLocator.locate(content, draft.sectionLabel(), draft.anchorText())
                .map(located -> new Positioned(
                        new ValidFindingDraft(
                                category.get(),
                                draft.sectionLabel() == null ? "" : draft.sectionLabel().trim(),
                                located.text(),
                                draft.title().trim(),
                                draft.reason().trim(),
                                draft.evidence().trim(),
                                draft.question().trim()
                        ),
                        located.start()
                ));
    }

    private static Optional<FindingCategory> parseCategory(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(FindingCategory.valueOf(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean hasBlank(FindingDraft draft) {
        return isBlank(draft.title()) || isBlank(draft.reason())
                || isBlank(draft.evidence()) || isBlank(draft.question());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Positioned(ValidFindingDraft draft, int start) {
    }
}
