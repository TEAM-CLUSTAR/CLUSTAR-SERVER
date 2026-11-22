package org.project.global.config.swagger;

import lombok.Getter;
import org.project.global.exception.errorcode.*;

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

    UPDATE_MEMO(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            MemoErrorCode.MEMO_IMAGE_NOT_FOUND,
            MemoErrorCode.MEMO_FILE_NOT_FOUND,
            MemoErrorCode.INVALID_ATTACHMENT_EDIT,
            MemoErrorCode.MISSING_ATTACHMENT_METADATA,
            MemoErrorCode.TOO_MANY_IMAGES,
            MemoErrorCode.TOO_MANY_FILES,
            MemoErrorCode.IMAGE_TOO_LARGE,
            MemoErrorCode.FILE_TOO_LARGE,
            MemoErrorCode.INVALID_S3_KEY_FORMAT,
            MemoErrorCode.S3_KEY_USER_MISMATCH
    ))),

    GOOGLE_LOGIN(new LinkedHashSet<>(Set.of(

    ))),

    GOOGLE_LOGIN_CALLBACK(new LinkedHashSet<>(Set.of(
            LoginErrorCode.AUTH_SOCIAL_LOGIN_FAIL,
            LoginErrorCode.NOT_FOUND_GOOGLE_ACCESS_TOKEN_RESPONSE
    ))),

    REISSUE_TOKEN(new LinkedHashSet<>(Set.of(
            LoginErrorCode.REFRESH_TOKEN_NOT_FOUND,
            LoginErrorCode.INVALID_REFRESH_TOKEN,
            UserErrorCode.NOT_FOUND_USER,
            LoginErrorCode.REFRESH_TOKEN_EXPIRED
    ))),

    LOGOUT(new LinkedHashSet<>(Set.of(
            LoginErrorCode.REFRESH_TOKEN_DELETE_FAILED,
            LoginErrorCode.ACCESS_TOKEN_BLACKLIST_FAILED
    ))),

    MEMO_AI(new LinkedHashSet<>(Set.of(
            AiErrorCode.UNSUPPORTED_TYPE,
            AiErrorCode.MEMO_NOT_FOUND,
            LoginErrorCode.ACCESS_TOKEN_BLACKLIST_FAILED,
            UserErrorCode.NOT_FOUND_USER,
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
            MemoErrorCode.FORBIDDEN_MEMO
    ))),

    GET_TRASH_MEMOS(new LinkedHashSet<>(Set.of(
            MemoErrorCode.INVALID_PAGE_SIZE,
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED
    ))),

    GET_TRASH_MEMO(new LinkedHashSet<>(Set.of(
            MemoErrorCode.TRASH_MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED
    ))),

    TRASH_MEMO_ACTION(new LinkedHashSet<>(Set.of(
            MemoErrorCode.TRASH_MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO
    ))),

    CREATE_AI_MEMO(new LinkedHashSet<>(Set.of(

    ))),

    GET_PRESIGNED_URLS(new LinkedHashSet<>(Set.of(
            S3ErrorCode.PRESIGNED_URL_GENERATION_FAILED,
            MemoErrorCode.UNSUPPORTED_EXTENSION,
            MemoErrorCode.TOO_MANY_IMAGES,
            MemoErrorCode.TOO_MANY_FILES,
            MemoErrorCode.IMAGE_TOO_LARGE,
            MemoErrorCode.FILE_TOO_LARGE
    ))),

    // == AI 검토 == //
    GET_ACTIVE_REVIEW(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO
    ))),
    START_REVIEW(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            ReviewErrorCode.CONTENT_TOO_SHORT,
            ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS,
            ReviewErrorCode.REVIEW_AI_FAILED
    ))),
    ANSWER_FINDING(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            ReviewErrorCode.REVIEW_NOT_FOUND,
            ReviewErrorCode.FINDING_NOT_FOUND,
            ReviewErrorCode.FINDING_ALREADY_APPLIED,
            ReviewErrorCode.ANSWER_TOO_LONG
    ))),
    REFINE_FINDING(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            ReviewErrorCode.REVIEW_NOT_FOUND,
            ReviewErrorCode.FINDING_NOT_FOUND,
            ReviewErrorCode.FINDING_ALREADY_APPLIED,
            ReviewErrorCode.ANSWER_REQUIRED,
            ReviewErrorCode.REVIEW_AI_FAILED
    ))),
    APPLY_FINDING(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            ReviewErrorCode.REVIEW_NOT_FOUND,
            ReviewErrorCode.FINDING_NOT_FOUND,
            ReviewErrorCode.FINDING_ALREADY_APPLIED,
            ReviewErrorCode.APPLY_TEXT_REQUIRED,
            ReviewErrorCode.ANSWER_REQUIRED,
            ReviewErrorCode.ANCHOR_NOT_FOUND
    ))),
    RERUN_REVIEW(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            ReviewErrorCode.REVIEW_NOT_FOUND,
            ReviewErrorCode.CONTENT_TOO_SHORT,
            ReviewErrorCode.REVIEW_ALREADY_IN_PROGRESS,
            ReviewErrorCode.REVIEW_AI_FAILED
    ))),
    END_REVIEW(new LinkedHashSet<>(Set.of(
            MemoErrorCode.MEMO_NOT_FOUND,
            MemoErrorCode.FORBIDDEN_MEMO,
            ReviewErrorCode.REVIEW_NOT_FOUND
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
