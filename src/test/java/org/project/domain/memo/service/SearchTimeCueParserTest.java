package org.project.domain.memo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.service.SearchTimeCueParser.DateOrder;
import org.project.domain.memo.service.SearchTimeCueParser.ParsedQuery;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SearchTimeCueParser 테스트")
class SearchTimeCueParserTest {

    // 2026-10-30은 금요일
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    private ParsedQuery parse(String query) {
        return SearchTimeCueParser.parse(query, TODAY);
    }

    @Test
    @DisplayName("시간 단서가 없으면 검색어를 그대로 돌려준다")
    void parse_noCue_returnsQueryAsIs() {
        ParsedQuery parsed = parse("푸시  알림");

        assertThat(parsed.hasCue()).isFalse();
        assertThat(parsed.text()).isEqualTo("푸시  알림");
    }

    @Test
    @DisplayName("지난주는 직전 월~일 한 주로 해석하고 단서는 검색어에서 뺀다")
    void parse_lastWeek() {
        ParsedQuery parsed = parse("지난주에 쓴 거 중에 엄마 나오는 메모");

        assertThat(parsed.from()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(parsed.toExclusive()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(parsed.text()).isEqualTo("거 중에 엄마 나오는 메모");
        assertThat(parsed.order()).isEqualTo(DateOrder.NONE);
    }

    @Test
    @DisplayName("어제, 오늘, 그저께를 하루 범위로 해석한다")
    void parse_days() {
        assertThat(parse("어제 쓴 산책 얘기").from()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(parse("어제 쓴 산책 얘기").toExclusive()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(parse("오늘 쓴 메모").from()).isEqualTo(TODAY);
        assertThat(parse("그저께 쓴 메모").from()).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    @DisplayName("이번 주, 이번 달, 지난달, 올해, 작년을 해석한다")
    void parse_periods() {
        assertThat(parse("이번 주에 쓴 일정").from()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(parse("이번 달에 쓴 지출").from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(parse("지난달에 쓴 지출").from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(parse("지난달에 쓴 지출").toExclusive()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(parse("올해 쓴 계획").from()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(parse("작년에 쓴 계획").toExclusive()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("N월은 올해 해당 월로, 아직 오지 않은 월이면 작년 해당 월로 해석한다")
    void parse_month() {
        ParsedQuery august = parse("8월에 쓴 가격 고민");
        assertThat(august.from()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(august.toExclusive()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(august.text()).isEqualTo("가격 고민");

        assertThat(parse("12월에 쓴 계획").from()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(parse("10월에 쓴 계획").from()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    @DisplayName("쓴/작성한 같은 표지가 뒤따르지 않으면 시간 단서로 보지 않는다")
    void parse_requiresWriteMarker() {
        assertThat(parse("오늘의 할 일").hasCue()).isFalse();
        assertThat(parse("12월 셋째 주 일정").hasCue()).isFalse();
        assertThat(parse("지난주 러닝 기록").hasCue()).isFalse();
        assertThat(parse("처음 들어온 사람한테 물어보는 게 너무 많다는 거").hasCue()).isFalse();
    }

    @Test
    @DisplayName("13월 같은 해석할 수 없는 월은 단서로 보지 않는다")
    void parse_invalidMonth() {
        assertThat(parse("13월에 쓴 메모").hasCue()).isFalse();
    }

    @Test
    @DisplayName("제일 처음, 처음 쓴은 오래된 순을 요구한다")
    void parse_earliest() {
        ParsedQuery first = parse("가입 절차 관련해서 제일 처음 쓴 메모");
        assertThat(first.order()).isEqualTo(DateOrder.EARLIEST);
        assertThat(first.hasRange()).isFalse();
        assertThat(first.text()).isEqualTo("가입 절차 관련해서 메모");

        assertThat(parse("처음 쓴 러닝 기록").order()).isEqualTo(DateOrder.EARLIEST);
        assertThat(parse("처음 쓴 러닝 기록").text()).isEqualTo("러닝 기록");
    }

    @Test
    @DisplayName("가장 최근에 쓴, 마지막으로 쓴은 최신 순을 요구한다")
    void parse_latest() {
        assertThat(parse("가장 최근에 쓴 요리 메모").order()).isEqualTo(DateOrder.LATEST);
        assertThat(parse("가장 최근에 쓴 요리 메모").text()).isEqualTo("요리 메모");
        assertThat(parse("마지막으로 쓴 일기").order()).isEqualTo(DateOrder.LATEST);
    }

    @Test
    @DisplayName("날짜 범위와 정렬 요구를 함께 해석한다")
    void parse_rangeAndOrder() {
        ParsedQuery parsed = parse("9월에 쓴 것 중 가장 최근에 쓴 회의");

        assertThat(parsed.from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(parsed.order()).isEqualTo(DateOrder.LATEST);
        assertThat(parsed.text()).isEqualTo("것 중 회의");
    }

    @Test
    @DisplayName("null이나 공백은 단서가 없는 것으로 본다")
    void parse_blank() {
        assertThat(parse(null).hasCue()).isFalse();
        assertThat(parse("  ").hasCue()).isFalse();
    }
}
