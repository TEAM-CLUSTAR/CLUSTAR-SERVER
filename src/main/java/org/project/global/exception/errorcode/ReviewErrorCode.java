package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReviewErrorCode implements ErrorCode {
    CONTENT_TOO_SHORT(HttpStatus.BAD_REQUEST, 400, "검토하려면 본문을 300자 이상 작성해 주세요."),
    REVIEW_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, 409, "이미 진행 중인 검토가 있습니다."),
    REVIEW_AI_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final int code;
    private final String msg;

    ReviewErrorCode(HttpStatus status, int code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }
}
