package org.project.domain.review.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// AI가 돌려준 문장(anchorText)을 본문에서 찾는다.
// 프롬프트로 "그대로 복사하라"고 해도 AI가 띄어쓰기·따옴표·말줄임표를 바꿔 올 때가 있어,
// 그 차이만 무시하고 비교한 뒤 본문에 실제로 적힌 문장을 돌려준다(글자가 다르면 못 찾음)
public final class AnchorTextLocator {

    private AnchorTextLocator() {
    }

    public static Optional<Located> locate(String content, String anchor) {
        if (content == null || anchor == null || anchor.isBlank()) {
            return Optional.empty();
        }

        String trimmed = anchor.trim();
        int exact = content.indexOf(trimmed);
        if (exact >= 0) {
            return Optional.of(new Located(trimmed, exact));
        }

        Normalized normalizedContent = normalize(content);
        String normalizedAnchor = normalize(trimmed).text();
        int index = normalizedContent.text().indexOf(normalizedAnchor);
        if (index < 0 || normalizedAnchor.isEmpty()) {
            return Optional.empty();
        }

        int start = normalizedContent.originIndex().get(index);
        int end = normalizedContent.originIndex().get(index + normalizedAnchor.length() - 1) + 1;
        return Optional.of(new Located(content.substring(start, end), start));
    }

    // 정규화: 연속 공백 → 공백 하나, 여러 모양의 따옴표 → ' 또는 ", 말줄임표 … → ...
    // 정규화된 글자마다 본문의 원래 위치를 기억해 둔다
    private static Normalized normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        List<Integer> origin = new ArrayList<>(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ' ') {
                    continue;
                }
                sb.append(' ');
                origin.add(i);
            } else if (c == '‘' || c == '’' || c == '‚' || c == '′') {
                sb.append('\'');
                origin.add(i);
            } else if (c == '“' || c == '”' || c == '„' || c == '″') {
                sb.append('"');
                origin.add(i);
            } else if (c == '…') {
                for (int k = 0; k < 3; k++) {
                    sb.append('.');
                    origin.add(i);
                }
            } else {
                sb.append(c);
                origin.add(i);
            }
        }
        return new Normalized(sb.toString(), origin);
    }

    // text: 본문에 실제로 적힌 문장, start: 본문에서의 시작 위치(본문 순서 정렬용)
    public record Located(String text, int start) {
    }

    private record Normalized(String text, List<Integer> originIndex) {
    }
}
