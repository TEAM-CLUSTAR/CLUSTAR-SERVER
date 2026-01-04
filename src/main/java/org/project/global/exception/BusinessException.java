package org.project.global.exception;

import lombok.Getter;
import org.project.global.exception.errorcode.GlobalErrorCode;

@Getter
public class BusinessException extends RuntimeException {  // 예외클래스 상속의 중간층
    private final GlobalErrorCode errorCode;

    public BusinessException(GlobalErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }
}
