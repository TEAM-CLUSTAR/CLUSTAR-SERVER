package org.project.domain.review.ai.dto;

import java.util.List;

// 분석 응답 JSON 전체. 빈틈이 없으면 빈 배열
public record AnalysisResult(List<FindingDraft> findings) {
}
