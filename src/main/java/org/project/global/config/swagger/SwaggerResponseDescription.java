package org.project.global.config.swagger;

import lombok.Getter;
import org.project.domain.user.errorcode.UserErrorCode;
import org.project.global.exception.errorcode.ErrorCode;
import org.project.global.exception.errorcode.GlobalErrorCode;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
// 에러 그룹
public enum SwaggerResponseDescription {

    GET_USER(new LinkedHashSet<>(Set.of(
            UserErrorCode.USER_NOT_FOUND
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