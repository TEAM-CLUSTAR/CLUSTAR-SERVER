package org.project.domain.review.ai.dto;

import java.util.List;

// 다듬기 응답 JSON. 질문 하나당 한 줄(라벨 + 문장)
public record RefineResult(List<RefinedLine> lines) {

    public record RefinedLine(String label, String sentence) {
    }
}
