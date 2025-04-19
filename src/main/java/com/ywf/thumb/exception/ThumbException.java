package com.ywf.thumb.exception;

import com.ywf.thumb.common.ErrorCode;

public class ThumbException extends RuntimeException {
    /**
     * 错误码
     */
    private final int code;

    public ThumbException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ThumbException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ThumbException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}