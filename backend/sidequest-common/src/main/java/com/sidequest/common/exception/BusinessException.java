package com.sidequest.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final String messageKey;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.code = errorCode.getCode();
        this.messageKey = errorCode.getMessageKey();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.messageKey = message;
    }
}

