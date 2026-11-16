package org.project.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchQueryTokenizer 테스트")
class SearchQueryTokenizerTest {

    @Test
    @DisplayName("null이나 공백이면 빈 목록을 반환한다")
    void tokenize_blank_returnsEmpty() {
        assertThat(SearchQueryTokenizer.tokenize(null)).isEmpty();
        assertThat(SearchQueryTokenizer.tokenize("   ")).isEmpty();
    }

    @Test
    @DisplayName("공백 기준으로 나누고 중복은 입력 순서를 유지한 채 제거한다")
    void tokenize_splitsAndDeduplicates() {
        assertThat(SearchQueryTokenizer.tokenize("청소  도구 청소")).containsExactly("청소", "도구");
    }

    @Test
    @DisplayName("끝 조사를 떼어 낸다")
    void tokenize_stripsTrailingParticle() {
        assertThat(SearchQueryTokenizer.tokenize("사람들이 떠났다는")).containsExactly("사람들", "떠났다");
        assertThat(SearchQueryTokenizer.tokenize("사람한테 공원에서")).containsExactly("사람", "공원");
    }

    @Test
    @DisplayName("조사를 떼고 남는 부분이 너무 짧으면 그대로 둔다")
    void tokenize_keepsShortStem() {
        assertThat(SearchQueryTokenizer.tokenize("아이")).containsExactly("아이");
        assertThat(SearchQueryTokenizer.tokenize("회의")).containsExactly("회의");
    }

    @Test
    @DisplayName("불용어는 제외한다")
    void tokenize_removesStopwords() {
        assertThat(SearchQueryTokenizer.tokenize("너무 많은 알림이 오는 거")).containsExactly("많은", "알림", "오는");
        assertThat(SearchQueryTokenizer.tokenize("엄마 나오는 메모")).containsExactly("엄마");
    }

    @Test
    @DisplayName("한 글자 명사는 불용어가 아니면 유지한다")
    void tokenize_keepsSingleCharacterNoun() {
        assertThat(SearchQueryTokenizer.tokenize("책 표지")).containsExactly("책", "표지");
    }

    @Test
    @DisplayName("불용어만 입력하면 정리 전 단어를 그대로 사용한다")
    void tokenize_onlyStopwords_fallsBackToRawTokens() {
        assertThat(SearchQueryTokenizer.tokenize("메모")).containsExactly("메모");
        assertThat(SearchQueryTokenizer.tokenize("너무 많이")).containsExactly("너무", "많이");
    }

    @Test
    @DisplayName("앞뒤 문장부호는 제거한다")
    void tokenize_stripsEdgePunctuation() {
        assertThat(SearchQueryTokenizer.tokenize("푸시 알림?")).containsExactly("푸시", "알림");
        assertThat(SearchQueryTokenizer.tokenize("A/B 테스트")).containsExactly("A/B", "테스트");
    }
}
