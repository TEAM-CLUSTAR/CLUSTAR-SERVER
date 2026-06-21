package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LabelErrorCode implements ErrorCode {

    PARENT_LABEL_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "부모 태그를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String msg;

    LabelErrorCode(HttpStatus status, int code, String msg) {
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
