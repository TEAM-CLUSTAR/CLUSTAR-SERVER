package org.project.domain.review.entity;

// 검토에서 AI를 부르는 종류
public enum ReviewAiCallType {
    // 글 전체를 읽고 빈틈 찾기
    ANALYSIS,
    // 첫 답을 보고 후속 질문 만들기
    FOLLOW_UP,
    // 답들을 문서 문장으로 다듬기
    REFINE
}
