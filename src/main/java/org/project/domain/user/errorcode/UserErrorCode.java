package org.project.domain.user.errorcode;

import lombok.Getter;
import org.project.global.exception.errorcode.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,404,"해당 ID의 유저를 찾을 수 없습니다.");

    private final HttpStatus status;

    private final int code;

    private final String msg;

    UserErrorCode(HttpStatus status, int code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }

}
