package org.project.domain.review.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

// 분석 응답 JSON 전체. 빈틈이 없으면 빈 배열
// documentGoal: AI가 빈틈을 고르기 전에 먼저 정리한 글의 목적. 저장하지 않고, 목적에 비춰 빈틈을 고르게 하는 용도
public record AnalysisResult(
        @JsonPropertyDescription("빈틈을 고르기 전에 먼저 쓴다. 이 글이 풀려는 문제나 이루려는 목표 한 문장")
        String documentGoal,

        @JsonPropertyDescription("글쓴이가 내린 결정의 빈틈만, 중요한 순서대로. 문장마다 채우지 않는다. 결정이 거의 없는 글이면 0~2개 또는 빈 배열")
        List<FindingDraft> findings
) {

    public AnalysisResult(List<FindingDraft> findings) {
        this(null, findings);
    }
}
