package org.project.domain.review.entity;

// 빈틈 카드 상태. '반영 대기'(미리보기)는 서버에 저장하지 않고 프론트 화면 상태로만 둔다
public enum FindingStatus {
    // 아직 답하지 않음
    OPEN,
    // 답을 1개 이상 보냄
    ANSWERING,
    // 본문에 반영함
    APPLIED
}
