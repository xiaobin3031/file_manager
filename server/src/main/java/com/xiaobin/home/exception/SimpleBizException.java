package com.xiaobin.home.exception;

import lombok.Getter;

/**
 * 简单业务异常，没有堆栈信息
 */

@Getter
public class SimpleBizException extends RuntimeException {

    private final int code;

    public SimpleBizException(int code, String message) {
        super(message, null, false, false);
        this.code = code;
    }

    public SimpleBizException(String message) {
        this(-1, message);
    }

    @Override
    public String toString() {
        return this.code + ", " + super.getMessage();
    }
}
