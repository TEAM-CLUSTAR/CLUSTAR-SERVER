package org.project.global.exception.domainException;

import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.ErrorCode;

public class S3CustomException extends BusinessException {
    public S3CustomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
