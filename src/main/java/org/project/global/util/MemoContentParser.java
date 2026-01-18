package org.project.global.util;

public final class MemoContentParser {

    private MemoContentParser() {
    }

    public static ParsedMemoContent parseTitleAndContent(String rawContent) {
        if (rawContent == null) {
            return new ParsedMemoContent("AI Memo", "");
        }

        String normalized = rawContent.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return new ParsedMemoContent("AI Memo", "");
        }

        int firstNewline = normalized.indexOf('\n');
        if (firstNewline < 0) {
            String title = normalized.trim();
            if (title.isEmpty()) {
                title = "AI Memo";
            }
            return new ParsedMemoContent(title, "");
        }

        String title = normalized.substring(0, firstNewline).trim();
        String content = normalized.substring(firstNewline + 1).trim();

        if (title.isEmpty()) {
            title = "AI Memo";
        }

        return new ParsedMemoContent(title, content);
    }

    public record ParsedMemoContent(
            String title,
            String content
    ) {
    }
}
