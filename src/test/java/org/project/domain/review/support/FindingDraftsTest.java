package org.project.domain.review.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.review.ai.dto.FindingDraft;
import org.project.domain.review.entity.FindingCategory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FindingDrafts 테스트")
class FindingDraftsTest {

    private static final String CONTENT = """
            2. 타깃
            전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.
            3. 거래 방식
            거래는 학생회관 사물함을 통한 비대면 방식으로 진행한다.
            4. 운영
            학생회가 사물함 관리를 맡는다.
            """;

    private static final String TARGET = "전공책이 필요한 1~2학년 재학생을 주 사용자로 한다.";
    private static final String LOCKER = "거래는 학생회관 사물함을 통한 비대면 방식으로 진행한다.";
    private static final String COUNCIL = "학생회가 사물함 관리를 맡는다.";

    @Test
    @DisplayName("본문 순서대로 정렬하고 유형 문자열을 enum으로 바꾼다")
    void sortsByPositionInContent() {
        List<ValidFindingDraft> result = FindingDrafts.sanitize(CONTENT, List.of(
                draft("STAKEHOLDER", COUNCIL),
                draft("CRITERIA", TARGET)
        ), 10);

        assertThat(result).extracting(ValidFindingDraft::anchorText).containsExactly(TARGET, COUNCIL);
        assertThat(result.get(0).category()).isEqualTo(FindingCategory.CRITERIA);
    }

    @Test
    @DisplayName("본문에 없는 문장, 모르는 유형, 빈 항목이 있는 카드는 버린다")
    void dropsInvalidDrafts() {
        List<ValidFindingDraft> result = FindingDrafts.sanitize(CONTENT, List.of(
                draft("CRITERIA", "본문에 없는 문장이다."),
                draft("UNKNOWN", TARGET),
                new FindingDraft("CRITERIA", "3. 거래 방식", LOCKER, "제목", " ", "근거", "질문"),
                draft("STAKEHOLDER", COUNCIL)
        ), 10);

        assertThat(result).extracting(ValidFindingDraft::anchorText).containsExactly(COUNCIL);
    }

    @Test
    @DisplayName("같은 문장을 두 번 짚으면 먼저 나온 것만 남긴다")
    void dropsDuplicateAnchor() {
        List<ValidFindingDraft> result = FindingDrafts.sanitize(CONTENT, List.of(
                draft("CRITERIA", TARGET),
                draft("EDGE_CASE", TARGET)
        ), 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(FindingCategory.CRITERIA);
    }

    @Test
    @DisplayName("최대 개수를 넘으면 AI가 준 순서(중요한 순)로 자른 뒤 본문 순서로 정렬한다")
    void limitsByAiOrderThenSortsByPosition() {
        List<ValidFindingDraft> result = FindingDrafts.sanitize(CONTENT, List.of(
                draft("STAKEHOLDER", COUNCIL),
                draft("CRITERIA", LOCKER),
                draft("CRITERIA", TARGET)
        ), 2);

        assertThat(result).extracting(ValidFindingDraft::anchorText).containsExactly(LOCKER, COUNCIL);
    }

    @Test
    @DisplayName("AI가 문장부호를 바꿔도 본문에 적힌 원래 문장으로 저장한다")
    void storesOriginalAnchorText() {
        List<ValidFindingDraft> result = FindingDrafts.sanitize(CONTENT, List.of(
                draft("CRITERIA", "전공책이 필요한  1~2학년 재학생을 주 사용자로 한다.")
        ), 10);

        assertThat(result.get(0).anchorText()).isEqualTo(TARGET);
    }

    @Test
    @DisplayName("같은 문장이 여러 장에 있으면 AI가 적은 소제목 위치로 순서를 정한다")
    void duplicatedSentence_sortsBySectionPosition() {
        String content = "1. 배경\n세부 내용은 추후 정한다.\n2. 타깃\n" + TARGET + "\n3. 거래 방식\n세부 내용은 추후 정한다.\n";

        List<ValidFindingDraft> result = FindingDrafts.sanitize(content, List.of(
                new FindingDraft("CRITERIA", "3. 거래 방식", "세부 내용은 추후 정한다.", "제목", "짚은 곳", "근거", "질문"),
                new FindingDraft("CRITERIA", "2. 타깃", TARGET, "제목", "짚은 곳", "근거", "질문")
        ), 10);

        assertThat(result).extracting(ValidFindingDraft::sectionLabel).containsExactly("2. 타깃", "3. 거래 방식");
    }

    private FindingDraft draft(String category, String anchorText) {
        return new FindingDraft(category, "섹션", anchorText, "제목", "짚은 곳", "근거", "질문");
    }
}
