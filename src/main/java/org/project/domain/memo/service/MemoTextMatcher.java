package org.project.domain.memo.service;

import org.project.domain.memo.entity.Memo;
import org.project.domain.tag.entity.Tag;

import java.util.List;

// 텍스트 검색 결과가 검색어와 얼마나 맞는지 판정하는 유틸 클래스
// 강한 일치: 검색어 전체 구문이 포함되거나 단어가 일정 비율 이상 포함, 약한 일치: 단어 일부만 포함
final class MemoTextMatcher {

    private MemoTextMatcher() {
    }

    static boolean isStrongMatch(Memo memo, List<String> tokens, double minTokenCoverage) {
        if (tokens.isEmpty()) {
            return false;
        }

        String title = lower(memo.getTitle());
        String content = lower(memo.getContent());
        List<String> tagNames = memo.getTags().stream()
                .map(Tag::getName)
                .map(MemoTextMatcher::lower)
                .toList();

        String phrase = String.join(" ", tokens).toLowerCase();
        if (title.contains(phrase) || content.contains(phrase) || tagNames.stream().anyMatch(name -> name.contains(phrase))) {
            return true;
        }

        long matched = tokens.stream()
                .map(String::toLowerCase)
                .filter(token -> title.contains(token) || content.contains(token)
                        || tagNames.stream().anyMatch(name -> name.contains(token)))
                .count();
        return (double) matched / tokens.size() >= minTokenCoverage;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
