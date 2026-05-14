package com.xiaobin.home.exception;

public class NotLoginException extends RuntimeException {

    public NotLoginException() {
        super("not login", null, false, false);
    }
}
