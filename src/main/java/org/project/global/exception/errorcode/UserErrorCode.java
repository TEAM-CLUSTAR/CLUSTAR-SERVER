package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements ErrorCode {

    /**
     * 404 NOT_FOUND
     */
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, 404, "존재하지 않는 사용자입니다.");

    private final HttpStatus status;
    private final int code;
    private final String msg;

    UserErrorCode(HttpStatus status, int code, String msg) {
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
