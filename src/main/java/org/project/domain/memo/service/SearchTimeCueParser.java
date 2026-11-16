package org.project.domain.memo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 검색어의 시간 단서("지난주에 쓴", "8월에 쓴", "제일 처음 쓴")를 날짜 조건과 정렬로 바꾸는 유틸 클래스
// 오탐 방지: "쓴/작성한/적은/기록한/남긴"이 바로 뒤따를 때만 단서로 봄 ("오늘의 할 일", "12월 일정"은 제외)
// 단서 부분은 검색어에서 빼고 text로 돌려줌, 단서가 없으면 검색어를 그대로 돌려줌
// 예) "8월에 쓴 가격 고민" -> text="가격 고민", 8/1 이상 9/1 미만
final class SearchTimeCueParser {

    enum DateOrder {
        NONE, EARLIEST, LATEST
    }

    // text: 단서를 뺀 검색어, from/toExclusive: 작성일 범위 [from, to) (없으면 null), order: 작성일 정렬 요구
    record ParsedQuery(String text, LocalDate from, LocalDate toExclusive, DateOrder order) {

        boolean hasRange() {
            return from != null || toExclusive != null;
        }

        boolean hasCue() {
            return hasRange() || order != DateOrder.NONE;
        }
    }

    private static final String WRITE = "(?:쓴|작성한|적은|기록한|남긴)";

    private static final Pattern DATE_CUE = Pattern.compile(
            "(?<!\\d)(?:(?<month>\\d{1,2})\\s*월"
                    + "|(?<word>그저께|그제|어제|오늘|이번\\s*주|이번\\s*달|(?:지난|저번)\\s*주|(?:지난|저번)\\s*달|올해|작년|지난해))"
                    + "(?:에는|에|중에|엔)?\\s*" + WRITE);

    private static final Pattern ORDER_CUE = Pattern.compile(
            "(?:(?<early>(?:제일|가장|맨)\\s*처음(?:으로)?|처음(?:으로)?|(?:가장|제일)\\s*오래전에?)"
                    + "|(?<late>(?:가장|제일)\\s*최근(?:에)?|최근에|마지막으로|맨\\s*마지막(?:으로)?))"
                    + "\\s*" + WRITE);

    private SearchTimeCueParser() {
    }

    static ParsedQuery parse(String query, LocalDate today) {
        if (query == null || query.isBlank()) {
            return new ParsedQuery(query, null, null, DateOrder.NONE);
        }

        String text = query;
        LocalDate from = null;
        LocalDate toExclusive = null;
        DateOrder order = DateOrder.NONE;

        Matcher date = DATE_CUE.matcher(text);
        while (date.find()) {
            LocalDate[] range = resolveRange(date, today);
            if (range != null) {
                from = range[0];
                toExclusive = range[1];
                text = remove(text, date);
                break;
            }
        }

        Matcher ordering = ORDER_CUE.matcher(text);
        if (ordering.find()) {
            order = ordering.group("early") != null ? DateOrder.EARLIEST : DateOrder.LATEST;
            text = remove(text, ordering);
        }

        if (from == null && order == DateOrder.NONE) {
            return new ParsedQuery(query, null, null, DateOrder.NONE);
        }
        return new ParsedQuery(text.trim().replaceAll("\\s+", " "), from, toExclusive, order);
    }

    private static String remove(String text, Matcher matcher) {
        return text.substring(0, matcher.start()) + " " + text.substring(matcher.end());
    }

    // 날짜 단서를 [하한, 상한) 범위로 변환, 해석할 수 없으면(13월 등) null
    private static LocalDate[] resolveRange(Matcher m, LocalDate today) {
        String month = m.group("month");
        if (month != null) {
            int value = Integer.parseInt(month);
            if (value < 1 || value > 12) {
                return null;
            }
            // 연도가 없으면 가장 최근의 해당 월 (아직 안 온 월이면 작년)
            int year = value > today.getMonthValue() ? today.getYear() - 1 : today.getYear();
            LocalDate first = LocalDate.of(year, value, 1);
            return new LocalDate[]{first, first.plusMonths(1)};
        }

        String word = m.group("word").replaceAll("\\s+", "");
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate firstOfYear = today.withDayOfYear(1);
        return switch (word) {
            case "오늘" -> new LocalDate[]{today, today.plusDays(1)};
            case "어제" -> new LocalDate[]{today.minusDays(1), today};
            case "그저께", "그제" -> new LocalDate[]{today.minusDays(2), today.minusDays(1)};
            case "이번주" -> new LocalDate[]{monday, monday.plusWeeks(1)};
            case "지난주", "저번주" -> new LocalDate[]{monday.minusWeeks(1), monday};
            case "이번달" -> new LocalDate[]{firstOfMonth, firstOfMonth.plusMonths(1)};
            case "지난달", "저번달" -> new LocalDate[]{firstOfMonth.minusMonths(1), firstOfMonth};
            case "올해" -> new LocalDate[]{firstOfYear, firstOfYear.plusYears(1)};
            case "작년", "지난해" -> new LocalDate[]{firstOfYear.minusYears(1), firstOfYear};
            default -> null;
        };
    }
}
