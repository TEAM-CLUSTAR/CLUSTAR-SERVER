package org.project.global.exception.domainException;

import org.project.global.exception.BusinessException;
import org.project.global.exception.errorcode.ErrorCode;

public class ChatRoomException extends BusinessException {
    public ChatRoomException(ErrorCode errorCode) {
        super(errorCode);
    }
}
