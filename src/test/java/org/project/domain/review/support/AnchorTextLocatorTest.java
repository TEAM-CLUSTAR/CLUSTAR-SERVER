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
        var located = AnchorTextLocator.locate(CONTENT, "", "판매자는 졸업 예정자와 3~4학년이다.");

        assertThat(located).isPresent();
        assertThat(located.get().text()).isEqualTo("판매자는 졸업 예정자와 3~4학년이다.");
        assertThat(located.get().start()).isEqualTo(CONTENT.indexOf("판매자는"));
    }

    @Test
    @DisplayName("AI가 띄어쓰기를 바꿔도 찾고, 본문에 적힌 원래 문장을 돌려준다")
    void whitespaceDifference_returnsOriginalText() {
        var located = AnchorTextLocator.locate(CONTENT, "", "판매자는  졸업 예정자와\n3~4학년이다.");

        assertThat(located).isPresent();
        assertThat(located.get().text()).isEqualTo("판매자는 졸업 예정자와 3~4학년이다.");
    }

    @Test
    @DisplayName("AI가 따옴표나 말줄임표 모양을 바꿔도 찾는다")
    void quoteAndEllipsisDifference() {
        var located = AnchorTextLocator.locate(CONTENT, "", "가격은 ‘정가의 30~50%’ 안에서 정한다…");

        assertThat(located).isPresent();
        assertThat(located.get().text()).isEqualTo("가격은 '정가의 30~50%' 안에서 정한다...");
    }

    @Test
    @DisplayName("AI가 글자를 바꾼 문장은 찾지 못한다")
    void changedWords_notFound() {
        assertThat(AnchorTextLocator.locate(CONTENT, "", "전공책이 필요한 1, 2학년 학생을 주 사용자로 한다.")).isEmpty();
    }

    @Test
    @DisplayName("빈 문장은 찾지 않는다")
    void blank_notFound() {
        assertThat(AnchorTextLocator.locate(CONTENT, "", "  ")).isEmpty();
        assertThat(AnchorTextLocator.locate(CONTENT, "", null)).isEmpty();
    }

    // 1장과 3장에 같은 문장이 있다
    private static final String DUPLICATED = """
            1. 배경
            세부 내용은 추후 정한다.
            2. 타깃
            1~2학년 재학생을 주 사용자로 한다.
            3. 거래 방식
            세부 내용은 추후 정한다.
            """;

    @Test
    @DisplayName("같은 문장이 여러 번 있으면 AI가 짚은 소제목 아래의 문장을 찾는다")
    void duplicatedSentence_findsInSection() {
        var located = AnchorTextLocator.locate(DUPLICATED, "3. 거래 방식", "세부 내용은 추후 정한다.");

        assertThat(located).isPresent();
        assertThat(located.get().start()).isEqualTo(DUPLICATED.lastIndexOf("세부 내용은 추후 정한다."));
    }

    @Test
    @DisplayName("띄어쓰기가 달라도 소제목 아래의 문장을 찾는다")
    void duplicatedSentence_whitespaceDifference_findsInSection() {
        var located = AnchorTextLocator.locate(DUPLICATED, "3. 거래 방식", "세부 내용은  추후 정한다.");

        assertThat(located).isPresent();
        assertThat(located.get().start()).isEqualTo(DUPLICATED.lastIndexOf("세부 내용은 추후 정한다."));
    }

    @Test
    @DisplayName("소제목이 없거나 본문에 없으면 처음 나오는 문장을 찾는다")
    void noSection_findsFirst() {
        int first = DUPLICATED.indexOf("세부 내용은 추후 정한다.");

        assertThat(AnchorTextLocator.locate(DUPLICATED, "", "세부 내용은 추후 정한다.").orElseThrow().start()).isEqualTo(first);
        assertThat(AnchorTextLocator.locate(DUPLICATED, "9. 없는 장", "세부 내용은 추후 정한다.").orElseThrow().start()).isEqualTo(first);
    }

    @Test
    @DisplayName("소제목 아래에 그 문장이 없으면(AI가 소제목을 잘못 적음) 처음 나오는 문장을 찾는다")
    void notInSection_findsFirst() {
        var located = AnchorTextLocator.locate(DUPLICATED, "3. 거래 방식", "1~2학년 재학생을 주 사용자로 한다.");

        assertThat(located).isPresent();
        assertThat(located.get().start()).isEqualTo(DUPLICATED.indexOf("1~2학년"));
    }

    @Test
    @DisplayName("소제목과 같은 말이 본문 중간에 먼저 나와도 줄 첫머리의 소제목을 기준으로 찾는다")
    void sectionWordInBody_usesHeadingLine() {
        String content = "1. 배경\n운영 방식은 추후 정한다.\n2. 운영\n운영 방식은 추후 정한다.\n";

        var located = AnchorTextLocator.locate(content, "2. 운영", "운영 방식은 추후 정한다.");

        assertThat(located.orElseThrow().start()).isEqualTo(content.lastIndexOf("운영 방식은"));
    }

    @Test
    @DisplayName("AI가 따옴표 모양을 바꿔 줘도, 뒤 장의 똑같은 문장보다 소제목 아래 문장을 먼저 찾는다")
    void quoteDifference_prefersSectionOverLaterExactMatch() {
        String content = "## 대상\n학생은 “동의”한다.\n## 운영\n학생은 \"동의\"한다.\n";

        var located = AnchorTextLocator.locate(content, "대상", "학생은 \"동의\"한다.");

        assertThat(located.orElseThrow().start()).isEqualTo(content.indexOf("학생은 “동의”"));
        assertThat(located.get().text()).isEqualTo("학생은 “동의”한다.");
    }

    @Test
    @DisplayName("소제목 \"운영\"을 찾을 때 \"운영 비용\" 줄이 아니라 줄 전체가 \"운영\"인 곳을 기준으로 한다")
    void sectionLabelPrefix_prefersWholeLineMatch() {
        String content = "## 운영 비용\n월 10만 원이 든다.\n## 운영\n월 10만 원이 든다.\n";

        var located = AnchorTextLocator.locate(content, "운영", "월 10만 원이 든다.");

        assertThat(located.orElseThrow().start()).isEqualTo(content.lastIndexOf("월 10만 원이 든다."));
    }
}
