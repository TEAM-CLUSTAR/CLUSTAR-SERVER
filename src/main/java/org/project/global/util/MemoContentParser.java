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

        // 본문 없이 한 줄만 온 경우. 약속된 형식(1행 제목 / 2행부터 본문)을 지키지 않은 응답이다.
        int firstNewline = normalized.indexOf('\n');
        if (firstNewline < 0) {
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
