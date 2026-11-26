package org.project.domain.review.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// AI가 돌려준 빈틈 하나(검증 전).
// 유형도 문자열로 받는다: AI가 모르는 값을 주면 파싱 전체가 실패하지 않고 그 카드만 버리기 위해서
// 필드 설명은 응답 스키마에 실려 Gemini가 각 칸을 쓸 때 규칙을 바로 옆에서 보게 한다
public record FindingDraft(
        @JsonPropertyDescription("질문이 묻는 것으로 정한다. 이유·기준이면 CRITERIA, 맡거나 영향받는 사람이면 STAKEHOLDER, 계획대로 안 될 때면 EDGE_CASE")
        String category,

        @JsonPropertyDescription("anchorText가 속한 소제목을 본문 표기 그대로. 없으면 빈 문자열")
        String sectionLabel,

        @JsonPropertyDescription("본문 문장 하나를 한 글자도 바꾸지 않고 통째로 복사. 소제목 줄 금지. 다른 카드와 겹치지 않게")
        String anchorText,

        @JsonPropertyDescription("빠진 것을 구체적으로 25자 안팎. 반드시 '~없어요' 또는 '~빠져 있어요'로 끝낸다. 질문형 금지")
        String title,

        @JsonPropertyDescription("글에 무엇이 정해져 있고 무엇이 빠졌는지 둘 다 1~2문장. 해요체(~어요/~예요)로 쓰고 '~습니다'는 쓰지 않는다")
        String reason,

        @JsonPropertyDescription("reason을 되풀이하지 않는다. 이 빈틈 때문에 무엇이 어긋나거나 판단하기 어려운지 1~2문장. 글의 다른 부분과 실제로 이어질 때만 그 표현을 인용하고 억지로 목표에 갖다 붙이지 않는다. 해요체, '~습니다' 금지")
        String evidence,

        @JsonPropertyDescription("묻는 대상이 하나인 짧은 열린 질문 1개, 해요체, 물음표로 끝. 사용자의 이유·생각을 묻고, '~할 계획인가요', '어떻게 처리할 건가요'처럼 계획이나 해결책을 요구하지 않는다")
        String question
) {
}
