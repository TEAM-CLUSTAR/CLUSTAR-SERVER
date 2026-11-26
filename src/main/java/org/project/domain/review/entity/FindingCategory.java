package org.project.domain.review.entity;

// 빈틈 유형
public enum FindingCategory {
    // 판단 기준: 결정의 이유나 기준이 없음
    CRITERIA,
    // 빠진 사람: 고려해야 할 이해관계자가 빠짐
    STAKEHOLDER,
    // 예외 상황: 일이 틀어지는 경우를 다루지 않음
    EDGE_CASE
}
