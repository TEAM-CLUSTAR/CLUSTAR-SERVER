package org.project.global.util;

import java.util.regex.Pattern;

public final class MarkdownUtil {
    private static final Pattern FENCED_CODE = Pattern.compile("(?s)```.*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`]*`");
    private static final Pattern IMAGES = Pattern.compile("!\\[(.*?)]\\((.*?)\\)");
    private static final Pattern LINKS = Pattern.compile("\\[(.*?)]\\((.*?)\\)");
    // 헤더 기호 제거
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s+");
    private static final Pattern BLOCKQUOTE = Pattern.compile("(?m)^\\s*>\\s?");
    // 리스트 기호 제거
    private static final Pattern UL_LIST = Pattern.compile("(?m)^\\s*[-*+]\\s+");
    // 같은 번호 리스트 기호 제거
    private static final Pattern OL_LIST = Pattern.compile("(?m)^\\s*\\d+\\.\\s+");
    // 구분선 제거
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("(?m)^\\s*([-*_]\\s*){3,}$");
    private static final Pattern BOLD = Pattern.compile("(\\*\\*|__)(.*?)\\1");
    private static final Pattern ITALIC = Pattern.compile("(\\*|_)(.*?)\\1");
    private static final Pattern STRIKETHROUGH = Pattern.compile("~~(.*?)~~");
    private static final Pattern TRAILING_SPACES = Pattern.compile("(?m)[ \\t]+$");
    private static final Pattern MULTI_SPACES = Pattern.compile("[ \\t]{2,}");
    private static final Pattern MULTI_NEWLINES = Pattern.compile("\\n{3,}");

    private MarkdownUtil() {}

    public static String strip(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;
        result = FENCED_CODE.matcher(result).replaceAll("");
        result = INLINE_CODE.matcher(result).replaceAll("");
        result = IMAGES.matcher(result).replaceAll("$1");
        result = LINKS.matcher(result).replaceAll("$1");
        result = HEADING.matcher(result).replaceAll("");
        result = BLOCKQUOTE.matcher(result).replaceAll("");
        result = UL_LIST.matcher(result).replaceAll("");
        result = OL_LIST.matcher(result).replaceAll("");
        result = HORIZONTAL_RULE.matcher(result).replaceAll("");
        result = STRIKETHROUGH.matcher(result).replaceAll("$1");
        result = BOLD.matcher(result).replaceAll("$2");
        result = ITALIC.matcher(result).replaceAll("$2");
        result = TRAILING_SPACES.matcher(result).replaceAll("");
        result = MULTI_SPACES.matcher(result).replaceAll(" ");
        result = MULTI_NEWLINES.matcher(result).replaceAll("\n\n");

        return result.trim();
    }
}
