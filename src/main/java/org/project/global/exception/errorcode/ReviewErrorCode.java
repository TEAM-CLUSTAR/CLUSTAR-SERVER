package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReviewErrorCode implements ErrorCode {
    // 글자 수 에러의 실제 응답 문구는 ReviewLengthException이 현재 글자 수와 기준으로 만든다(아래는 Swagger 예시용)
    CONTENT_TOO_SHORT(HttpStatus.BAD_REQUEST, 400, "본문이 짧아 검토할 수 없어요. 최소 글자 수 이상 작성해 주세요."),
    REVIEW_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, 409, "이미 진행 중인 검토가 있습니다."),
    REVIEW_AI_FAILED(HttpStatus.SERVICE_UNAVAILABLE, 503, "AI가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "진행 중인 검토를 찾을 수 없습니다."),
    FINDING_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 빈틈을 찾을 수 없습니다."),
    FINDING_ALREADY_APPLIED(HttpStatus.CONFLICT, 409, "이미 반영된 빈틈입니다."),
    ANSWER_TOO_LONG(HttpStatus.BAD_REQUEST, 400, "답변이 너무 길어요. 최대 글자 수 이하로 줄여 주세요."),
    ANSWER_REQUIRED(HttpStatus.BAD_REQUEST, 400, "답을 1개 이상 작성해야 반영할 수 있어요.");

    private final HttpStatus status;
    private final int code;
    private final String msg;

    ReviewErrorCode(HttpStatus status, int code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }
}
