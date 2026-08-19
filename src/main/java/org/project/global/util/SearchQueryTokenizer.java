package org.project.global.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 텍스트 검색용 검색어 토크나이저 유틸 클래스
// 공백 기준으로 단어를 나누고, 빈 토큰 제거 + 중복 제거(입력 순서 유지)
// 예) "청소  도구 청소" -> ["청소", "도구"]
public final class SearchQueryTokenizer {

    private SearchQueryTokenizer() {
    }

    public static List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : query.trim().split("\\s+")) {
            if (!raw.isBlank() && seen.add(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }
}
