package org.project.global.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatRoomErrorCode implements ErrorCode {

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "채팅방이 존재하지 않습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, 403, "채팅방에 대한 권한이 없습니다."),
    CHAT_ROOM_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 400, "이미 삭제된 채팅방입니다.");

    private final HttpStatus status;
    private final int code;
    private final String msg;

    ChatRoomErrorCode(HttpStatus status, int code, String msg) {
        this.status = status;
        this.code = code;
        this.msg = msg;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
