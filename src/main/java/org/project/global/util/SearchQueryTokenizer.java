package org.project.global.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 텍스트 검색용 검색어 토크나이저 유틸 클래스
// 공백 기준으로 나눈 뒤 앞뒤 문장부호, 끝 조사, 불용어를 제거하고 중복도 제거한다(입력 순서 유지).
// 예) "청소  도구 청소" -> ["청소", "도구"], "사람들이 떠났다는 거" -> ["사람들", "떠났다"]
// 정리 후 남는 단어가 없으면 정리 전 단어를 그대로 쓴다.
public final class SearchQueryTokenizer {

    // 조사를 뗀 뒤 남아야 하는 최소 길이 ("아이" -> "아" 방지)
    private static final int MIN_STEM_LENGTH = 2;

    private static final List<String> PARTICLES = List.of(
            "에서는", "으로는", "이라는",
            "에서", "으로", "까지", "부터", "에게", "한테", "보다", "처럼", "이랑", "하고", "에는", "에도", "이나", "라는",
            "은", "는", "이", "가", "을", "를", "에", "로", "도", "과", "와", "의", "만", "랑"
    ).stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();

    // 특정 메모를 가리키지 못하는 말 (지시어, 부사, "메모" 등)
    private static final Set<String> STOPWORDS = Set.of(
            "거", "게", "걸", "것", "수", "등", "좀", "더", "또", "및", "내", "나", "저", "제", "중", "때", "데", "곳",
            "너무", "정말", "진짜", "아주", "매우", "그냥", "조금", "많이", "계속", "다시", "먼저", "가장", "제일",
            "관련", "관련해서", "관한", "대한", "대해", "대해서", "통해", "위한", "위해", "때문", "때문에",
            "있는", "있던", "있다", "없는", "하는", "하던", "했던", "했다", "한다", "하다", "되는", "같은", "같이",
            "그리고", "그런", "이런", "저런", "어떤", "어떻게", "이거", "그거", "저거",
            "중에", "쓴", "나오는", "나오", "메모", "내용"
    );

    private SearchQueryTokenizer() {
    }

    public static List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Set<String> raws = new LinkedHashSet<>();
        Set<String> cleaned = clean(query, raws);
        return new ArrayList<>(cleaned.isEmpty() ? raws : cleaned);
    }

    // 불용어를 빼고도 검색할 단어가 남는지 (tokenize는 불용어뿐이어도 원래 단어를 반환하므로 구분용)
    public static boolean hasSearchableToken(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return !clean(query, new LinkedHashSet<>()).isEmpty();
    }

    // 정리된 단어를 반환하고, 정리 전 단어는 raws에 담음
    private static Set<String> clean(String query, Set<String> raws) {
        Set<String> cleaned = new LinkedHashSet<>();
        for (String word : query.trim().split("\\s+")) {
            String raw = stripEdgePunctuation(word);
            if (raw.isBlank()) {
                continue;
            }
            raws.add(raw);

            String stem = stripParticle(raw);
            if (STOPWORDS.contains(raw) || STOPWORDS.contains(stem)) {
                continue;
            }
            cleaned.add(stem);
        }
        return cleaned;
    }

    private static String stripEdgePunctuation(String word) {
        return word.replaceAll("^\\p{P}+|\\p{P}+$", "");
    }

    private static String stripParticle(String token) {
        for (String particle : PARTICLES) {
            if (token.endsWith(particle) && token.length() - particle.length() >= MIN_STEM_LENGTH) {
                return token.substring(0, token.length() - particle.length());
            }
        }
        return token;
    }
}
