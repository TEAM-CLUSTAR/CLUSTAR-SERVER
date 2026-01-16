package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum S3ErrorCode implements ErrorCode {
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "파일 삭제에 실패했습니다."),
    PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "URL 생성에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "S3에 존재하지 않는 파일입니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 500, "S3 파일 다운로드에 실패했습니다.");


    private final HttpStatus status;
    private final int code;
    private final String msg;
    S3ErrorCode(HttpStatus status, int code, String msg) {
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
