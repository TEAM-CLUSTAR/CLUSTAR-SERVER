package org.project.global.exception.domainException;

import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.ErrorCode;

public class LoginException extends BusinessException {
    public LoginException(ErrorCode errorCode) {
        super(errorCode);
    }
}
