package org.project.global.exception.domainException;

import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.ErrorCode;

public class AiException extends BusinessException {
    public AiException(ErrorCode errorCode) {
        super(errorCode);
    }
}

