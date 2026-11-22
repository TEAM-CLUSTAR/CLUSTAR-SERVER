package org.project.domain.review.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppliedTextInserter 테스트")
class AppliedTextInserterTest {

    private static final String CONTENT = """
            2. 타깃
            전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.
            판매자는 졸업 예정자와 3~4학년이다.""";

    @Test
    @DisplayName("짚은 문장이 있는 줄 바로 다음 줄에 넣는다")
    void insertsAfterAnchorLine() {
        String result = AppliedTextInserter.insertAfterAnchorLine(
                CONTENT, "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.", "선정 기준: 많이 사기 때문이다.").orElseThrow();

        assertThat(result).isEqualTo("""
                2. 타깃
                전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.
                선정 기준: 많이 사기 때문이다.
                판매자는 졸업 예정자와 3~4학년이다.""");
    }

    @Test
    @DisplayName("짚은 문장이 마지막 줄이면 본문 끝에 새 줄로 붙인다")
    void insertsAtEndWhenLastLine() {
        String result = AppliedTextInserter.insertAfterAnchorLine(
                CONTENT, "판매자는 졸업 예정자와 3~4학년이다.", "판매자 기준: 책이 남는 학년이다.").orElseThrow();

        assertThat(result).endsWith("판매자는 졸업 예정자와 3~4학년이다.\n판매자 기준: 책이 남는 학년이다.");
    }

    @Test
    @DisplayName("짚은 문장이 줄 중간에 있어도 그 줄이 끝난 다음에 넣는다(문장을 쪼개지 않음)")
    void insertsAfterWholeLine() {
        String content = "학생회가 사물함 관리를 맡는다. 비밀번호는 문자로 보낸다.\n다음 줄";

        String result = AppliedTextInserter.insertAfterAnchorLine(content, "학생회가 사물함 관리를 맡는다.", "추가 문장").orElseThrow();

        assertThat(result).isEqualTo("학생회가 사물함 관리를 맡는다. 비밀번호는 문자로 보낸다.\n추가 문장\n다음 줄");
    }

    @Test
    @DisplayName("반영 문장이 여러 줄이면 그대로 여러 줄로 들어간다")
    void insertsMultiLineText() {
        String result = AppliedTextInserter.insertAfterAnchorLine(
                CONTENT, "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.", "줄1\n줄2").orElseThrow();

        assertThat(result).contains("주 사용자로 한다.\n줄1\n줄2\n판매자는");
    }

    @Test
    @DisplayName("본문에서 짚은 문장을 찾지 못하면 비어 있다")
    void anchorMissing_empty() {
        assertThat(AppliedTextInserter.insertAfterAnchorLine(CONTENT, "지워진 문장이다.", "추가")).isEmpty();
    }
}
