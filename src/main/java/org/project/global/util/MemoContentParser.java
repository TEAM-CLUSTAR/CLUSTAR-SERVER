package org.project.global.util;

import org.project.global.exception.domainException.AiException;
import org.project.global.exception.errorcode.AiErrorCode;

public final class MemoContentParser {

    private MemoContentParser() {
    }

    public static ParsedMemoContent parseTitleAndContent(String rawContent) {
        if (rawContent == null) {
            throw new AiException(AiErrorCode.AI_RESPONSE_NULL);
        }

        String normalized = rawContent.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            throw new AiException(AiErrorCode.AI_RESPONSE_EMPTY);
        }

        int firstNewline = normalized.indexOf('\n');
        if (firstNewline < 0) {
            String title = normalized.trim();
            if (title.isEmpty()) {
                title = "AI Memo";
            }
            throw new AiException(AiErrorCode.AI_TITLE_EXTRACTION_FAILED);
        }

        String title = normalized.substring(0, firstNewline).trim();
        String content = normalized.substring(firstNewline + 1).trim();

        if (title.isEmpty()) {
            throw new AiException(AiErrorCode.AI_TITLE_EMPTY);
        }

        return new ParsedMemoContent(title, content);
    }

    public record ParsedMemoContent(
            String title,
            String content
    ) {
    }
}
