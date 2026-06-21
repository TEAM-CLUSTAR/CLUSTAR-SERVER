package org.project.global.exception.domainException;

import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.ErrorCode;

public class LabelException extends BusinessException {
    public LabelException(ErrorCode errorCode) {
        super(errorCode);
    }
}
