package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemoErrorCode implements ErrorCode {
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND,404,"해당 메모를 찾을 수 없습니다."),
    FORBIDDEN_MEMO(HttpStatus.FORBIDDEN, 403, "해당 메모에 접근할 권한이 없습니다."),
    INVALID_S3_KEY_FORMAT(HttpStatus.BAD_REQUEST, 400, "S3 키 형식이 올바르지 않습니다."),
    S3_KEY_USER_MISMATCH(HttpStatus.FORBIDDEN, 403, "요청한 사용자와 S3 리소스 소유자가 일치하지 않습니다.");;

    private final HttpStatus status;
    private final int code;
    private final String msg;

    MemoErrorCode(HttpStatus status, int code, String msg) {
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
