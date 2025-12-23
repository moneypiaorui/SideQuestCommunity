package com.sidequest.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    INTERNAL_ERROR(500, "error.internal"),
    POST_NOT_FOUND(1001, "error.post.not_found"),
    USER_BANNED(2001, "error.user.banned"),
    PARAM_ERROR(400, "error.param"),
    UNAUTHORIZED(401, "error.unauthorized"),
    FORBIDDEN(403, "error.forbidden");

    private final int code;
    private final String messageKey;

    ErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }
}

