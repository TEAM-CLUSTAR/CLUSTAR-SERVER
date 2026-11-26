package org.project.domain.review.support;

import java.util.Optional;

// 반영 문장을 본문에 넣는다. 메모 본문 형식(줄바꿈 텍스트)은 그대로 두고,
// 짚은 문장이 있는 줄이 끝난 바로 다음 줄에 새 줄로 넣는다(문장 중간을 쪼개지 않음)
public final class AppliedTextInserter {

    private AppliedTextInserter() {
    }

    // 같은 문장이 여러 장에 있으면 카드의 소제목 아래 문장을 기준으로 한다
    public static Optional<String> insertAfterAnchorLine(String content, String sectionLabel, String anchorText, String appliedText) {
        return AnchorTextLocator.locate(content, sectionLabel, anchorText)
                .map(located -> {
                    int anchorEnd = located.start() + located.text().length();
                    int lineEnd = content.indexOf('\n', anchorEnd);
                    if (lineEnd < 0) {
                        return content + "\n" + appliedText;
                    }
                    return content.substring(0, lineEnd) + "\n" + appliedText + content.substring(lineEnd);
                });
    }
}
