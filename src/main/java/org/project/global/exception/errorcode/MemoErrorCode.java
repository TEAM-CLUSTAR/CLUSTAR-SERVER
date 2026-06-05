package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemoErrorCode implements ErrorCode {
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND,404,"해당 메모를 찾을 수 없습니다."),
    SOURCE_MEMO_NOT_FOUND(HttpStatus.NOT_FOUND,404,"참고한 메모 ID 중 일부를 찾을 수 없습니다."),
    FORBIDDEN_MEMO(HttpStatus.FORBIDDEN, 403, "해당 메모에 접근할 권한이 없습니다."),
    INVALID_S3_KEY_FORMAT(HttpStatus.BAD_REQUEST, 400, "S3 키 형식이 올바르지 않습니다."),
    S3_KEY_USER_MISMATCH(HttpStatus.FORBIDDEN, 403, "요청한 사용자와 S3 리소스 소유자가 일치하지 않습니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, 400, "이미지는 최대 5개까지 업로드할 수 있습니다."),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST, 400, "파일은 최대 5개까지 업로드할 수 있습니다."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, 400, "이미지 용량은 최대 5MB까지 가능합니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, 400, "파일 용량은 최대 10MB까지 가능합니다."),
    MEMO_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 메모 이미지를 찾을 수 없습니다."),
    MEMO_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "해당 메모 파일을 찾을 수 없습니다."),
    EMPTY_SEARCH_QUERY(HttpStatus.BAD_REQUEST, 400, "검색어를 입력해주세요.");

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
