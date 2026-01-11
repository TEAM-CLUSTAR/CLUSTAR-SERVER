package org.project.global.exception.domainException;

import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.ErrorCode;

public class UserException extends BusinessException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
