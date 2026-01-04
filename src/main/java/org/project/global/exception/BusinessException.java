package org.project.global.exception;

import lombok.Getter;
import org.project.global.exception.errorcode.ErrorCode;

@Getter
public class BusinessException extends RuntimeException {  // 예외클래스 상속의 중간층
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }
}