package org.project.domain.ai.entity;

public enum ChatMessageStatus {

    /** 정상 저장된 메시지. USER 메시지는 항상 이 상태다. */
    SUCCESS,

    /** AI 호출이 실패해 응답을 받지 못한 자리. content가 없다. */
    FAILED
}
