package org.project.global.config.swagger;

import lombok.Getter;
import org.project.global.exception.errorcode.ErrorCode;
import org.project.global.exception.errorcode.GlobalErrorCode;
import org.project.global.exception.errorcode.LoginErrorCode;
import org.project.global.exception.errorcode.UserErrorCode;
import org.project.global.exception.errorcode.MemoErrorCode;
import org.project.global.exception.errorcode.S3ErrorCode;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
// 에러 그룹
public enum SwaggerResponseDescription {

    GET_USER(new LinkedHashSet<>(Set.of(
            UserErrorCode.NOT_FOUND_USER
    ))),

    CREATE_MEMO(new LinkedHashSet<>(Set.of(
            UserErrorCode.NOT_FOUND_USER
    ))),

    REISSUE_TOKEN(new LinkedHashSet<>(Set.of(
            LoginErrorCode.REFRESH_TOKEN_NOT_FOUND,
            LoginErrorCode.INVALID_REFRESH_TOKEN,
            UserErrorCode.NOT_FOUND_USER,
            LoginErrorCode.REFRESH_TOKEN_EXPIRED
            UserErrorCode.USER_NOT_FOUND,
            MemoErrorCode.INVALID_S3_KEY_FORMAT,
            MemoErrorCode.S3_KEY_USER_MISMATCH,
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED
    ))),

    GET_MEMOS(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED
    ))),

    GET_ONE_MEMO(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED
    ))),

    DELETE_MEMO(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            S3ErrorCode.FILE_DELETE_FAILED
    ))),

    CREATE_AI_MEMO(new LinkedHashSet<>(Set.of(

    ))),

    GET_PRESIGNED_URLS(new LinkedHashSet<>(Set.of(
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED
    )));

    private final Set<ErrorCode> errorCodeList;

    SwaggerResponseDescription(Set<ErrorCode> specificErrorCodes) {
        this.errorCodeList = new LinkedHashSet<>();
        this.errorCodeList.addAll(specificErrorCodes);
        this.errorCodeList.addAll(getGlobalErrorCodes());
    }

    private Set<ErrorCode> getGlobalErrorCodes() {
        return new LinkedHashSet<>(Set.of(GlobalErrorCode.values()));
    }
}
