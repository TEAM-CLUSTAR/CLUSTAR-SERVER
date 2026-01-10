package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum LoginErrorCode implements ErrorCode {

    /**
     * 401 UNAUTHORIZED
     */
    AUTH_SOCIAL_LOGIN_FAIL(HttpStatus.UNAUTHORIZED, 401, "유효하지 않은 인가 코드입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, 401, "리프레시 토큰이 존재하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 401, "리프레시 토큰이 유효하지 않습니다."),

    /**
     * 404 NOT FOUND
     */
    // 구글 엑세스 토큰 관련
    NOT_FOUND_GOOGLE_ACCESS_TOKEN_RESPONSE(HttpStatus.NOT_FOUND, 404, "구글 액세스 토큰 응답을 찾을 수 없습니다."),
    ;


    private final HttpStatus status;
    private final int code;
    private final String msg;

    LoginErrorCode(HttpStatus status, int code, String msg) {
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
