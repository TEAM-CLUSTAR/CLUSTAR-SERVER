package org.project.domain.review.dto.request;

// 반영 방식
public enum ApplyType {
    // [문서에 반영]: 미리보기에서 (고친) 다듬은 문장을 넣는다
    REFINED,
    // [원문 그대로 넣기]: 서버에 저장된 내 답을 그대로 넣는다
    RAW
}
