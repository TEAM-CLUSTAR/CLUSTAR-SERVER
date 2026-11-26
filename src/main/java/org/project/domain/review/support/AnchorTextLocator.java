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

    // 같은 문장이 여러 장에 있을 수 있어 AI가 적은 소제목 아래부터 먼저 찾는다.
    // 소제목이 없거나 그 아래에 문장이 없으면 본문 처음부터 찾는다
    public static Optional<Located> locate(String content, String sectionLabel, String anchor) {
        if (content == null || anchor == null || anchor.isBlank()) {
            return Optional.empty();
        }

        String trimmed = anchor.trim();
        int sectionStart = sectionStart(content, sectionLabel);
        if (sectionStart > 0) {
            Optional<Located> inSection = find(content, trimmed, sectionStart);
            if (inSection.isPresent()) {
                return inSection;
            }
        }
        return find(content, trimmed, 0);
    }

    // from 위치부터 처음 나오는 문장
    private static Optional<Located> find(String content, String anchor, int from) {
        int exact = content.indexOf(anchor, from);
        if (exact >= 0) {
            return Optional.of(new Located(anchor, exact));
        }

        Normalized normalizedContent = normalize(content);
        String normalizedAnchor = normalize(anchor).text();
        if (normalizedAnchor.isEmpty()) {
            return Optional.empty();
        }
        List<Integer> origin = normalizedContent.originIndex();
        int index = normalizedContent.text().indexOf(normalizedAnchor);
        while (index >= 0 && origin.get(index) < from) {
            index = normalizedContent.text().indexOf(normalizedAnchor, index + 1);
        }
        if (index < 0) {
            return Optional.empty();
        }

        int start = origin.get(index);
        int end = origin.get(index + normalizedAnchor.length() - 1) + 1;
        return Optional.of(new Located(content.substring(start, end), start));
    }

    // 소제목 줄의 위치. 같은 말이 본문 중간에도 나올 수 있어 줄 첫머리(앞에 공백·#만 있는 곳)를 먼저 보고,
    // 없으면 처음 나오는 곳을 쓴다. 못 찾으면 -1
    private static int sectionStart(String content, String sectionLabel) {
        if (sectionLabel == null || sectionLabel.isBlank()) {
            return -1;
        }
        String label = sectionLabel.trim();
        for (int index = content.indexOf(label); index >= 0; index = content.indexOf(label, index + 1)) {
            if (isLineHead(content, index)) {
                return index;
            }
        }
        return content.indexOf(label);
    }

    private static boolean isLineHead(String content, int index) {
        for (int i = index - 1; i >= 0 && content.charAt(i) != '\n'; i--) {
            char c = content.charAt(i);
            if (!Character.isWhitespace(c) && c != '#') {
                return false;
            }
        }
        return true;
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
