package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AiErrorCode implements ErrorCode {

    EMPTY_EMBEDDING_TEXT(HttpStatus.BAD_REQUEST, 400, "Embedding 대상 텍스트가 비어 있습니다."),
    UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 타입입니다."),
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "선택한 메모를 찾을 수 없습니다."),
    EMPTY_MEMO_IDS(HttpStatus.BAD_REQUEST, 400, "참조할 메모가 선택되지 않았습니다."),
    CONVERSATION_CONTEXT_NOT_SET(HttpStatus.BAD_REQUEST, 400, "conversation context is not set (userId / chatRoomId)"),
    AI_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요."),
    // 아래 네 가지는 모두 AI가 약속된 응답 형식(1행 제목 / 2행부터 본문)을 지키지 않은 경우다.
    AI_RESPONSE_EMPTY(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),
    AI_RESPONSE_NULL(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),
    AI_TITLE_EXTRACTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),
    AI_TITLE_EMPTY(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI 응답을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."),;

    private final HttpStatus status;
    private final int code;
    private final String msg;

    AiErrorCode(HttpStatus status, int code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
