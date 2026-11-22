package org.project.domain.review.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnchorTextLocator 테스트")
class AnchorTextLocatorTest {

    private static final String CONTENT = """
            2. 타깃
            전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.
            판매자는 졸업 예정자와 3~4학년이다.
            가격은 '정가의 30~50%' 안에서 정한다...
            """;

    @Test
    @DisplayName("본문에 그대로 있는 문장은 그 위치와 함께 찾는다")
    void exactMatch() {
        var located = AnchorTextLocator.locate(CONTENT, "판매자는 졸업 예정자와 3~4학년이다.");

        assertThat(located).isPresent();
        assertThat(located.get().text()).isEqualTo("판매자는 졸업 예정자와 3~4학년이다.");
        assertThat(located.get().start()).isEqualTo(CONTENT.indexOf("판매자는"));
    }

    @Test
    @DisplayName("AI가 띄어쓰기를 바꿔도 찾고, 본문에 적힌 원래 문장을 돌려준다")
    void whitespaceDifference_returnsOriginalText() {
        var located = AnchorTextLocator.locate(CONTENT, "판매자는  졸업 예정자와\n3~4학년이다.");

        assertThat(located).isPresent();
        assertThat(located.get().text()).isEqualTo("판매자는 졸업 예정자와 3~4학년이다.");
    }

    @Test
    @DisplayName("AI가 따옴표나 말줄임표 모양을 바꿔도 찾는다")
    void quoteAndEllipsisDifference() {
        var located = AnchorTextLocator.locate(CONTENT, "가격은 ‘정가의 30~50%’ 안에서 정한다…");

        assertThat(located).isPresent();
        assertThat(located.get().text()).isEqualTo("가격은 '정가의 30~50%' 안에서 정한다...");
    }

    @Test
    @DisplayName("AI가 글자를 바꾼 문장은 찾지 못한다")
    void changedWords_notFound() {
        assertThat(AnchorTextLocator.locate(CONTENT, "전공책이 필요한 1, 2학년 학생을 주 사용자로 한다.")).isEmpty();
    }

    @Test
    @DisplayName("빈 문장은 찾지 않는다")
    void blank_notFound() {
        assertThat(AnchorTextLocator.locate(CONTENT, "  ")).isEmpty();
        assertThat(AnchorTextLocator.locate(CONTENT, null)).isEmpty();
    }
}
