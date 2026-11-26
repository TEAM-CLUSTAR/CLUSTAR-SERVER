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

    // from 위치부터 가장 가까운 문장. 띄어쓰기·따옴표 차이를 무시하고 비교한다
    // (글자까지 같은 문장을 먼저 찾으면, 따옴표만 다른 소제목 아래 문장을 건너뛰고 뒤 장의 문장을 잡는다)
    private static Optional<Located> find(String content, String anchor, int from) {
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

    // 소제목 줄의 위치. 우선순위: 줄 전체가 소제목인 곳("## 운영") → 줄 첫머리로 시작하는 곳("## 운영 비용")
    // → 아무 데나 처음 나오는 곳. 못 찾으면 -1
    private static int sectionStart(String content, String sectionLabel) {
        if (sectionLabel == null || sectionLabel.isBlank()) {
            return -1;
        }
        String label = sectionLabel.trim();
        int lineHead = -1;
        for (int index = content.indexOf(label); index >= 0; index = content.indexOf(label, index + 1)) {
            if (!isLineHead(content, index)) {
                continue;
            }
            if (isLineEnd(content, index + label.length())) {
                return index;
            }
            if (lineHead < 0) {
                lineHead = index;
            }
        }
        return lineHead >= 0 ? lineHead : content.indexOf(label);
    }

    // index부터 줄 끝까지 공백뿐인지
    private static boolean isLineEnd(String content, int index) {
        for (int i = index; i < content.length() && content.charAt(i) != '\n'; i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                return false;
            }
        }
        return true;
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
