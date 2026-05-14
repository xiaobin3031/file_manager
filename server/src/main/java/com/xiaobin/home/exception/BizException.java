package com.xiaobin.home.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(-1, message);
    }

    @Override
    public String toString() {
        return this.code + ", " + super.getMessage();
    }
}
