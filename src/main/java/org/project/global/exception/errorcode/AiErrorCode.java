package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AiErrorCode implements ErrorCode {

    EMPTY_EMBEDDING_TEXT(HttpStatus.BAD_REQUEST, 400, "Embedding 대상 텍스트가 비어 있습니다."),
    UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, 400, "지원하지 않는 타입입니다."),
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "선택한 메모를 찾을 수 없습니다."),
    RAG_CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "RAG 검색 컨텍스트를 찾을 수 없습니다.");


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
