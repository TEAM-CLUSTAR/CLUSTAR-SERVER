package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LabelErrorCode implements ErrorCode {

    LABEL_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "태그를 찾을 수 없습니다."),
    PARENT_LABEL_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "부모 태그를 찾을 수 없습니다."),
    LABEL_ALREADY_EXISTS(HttpStatus.CONFLICT, 409, "이미 존재하는 태그명입니다."),
    LABEL_DEPTH_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, 400, "태그는 최대 3단계까지만 생성할 수 있습니다."),
    INVALID_PARENT_LABEL_ID(HttpStatus.BAD_REQUEST, 400, "부모 태그 ID는 1 이상이어야 합니다.");

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
