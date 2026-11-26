package org.project.domain.review.entity;

// 검토 한 번(= 버전 하나)의 상태
public enum ReviewStatus {
    // 화면에 떠 있는 검토. 메모당 최대 1개
    ACTIVE,
    // 다시 검토했거나 종료한 버전. 기록으로만 남는다
    ENDED
}
