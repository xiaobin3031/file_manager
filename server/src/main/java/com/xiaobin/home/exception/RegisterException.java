package com.xiaobin.home.exception;

public class RegisterException extends RuntimeException {

    public RegisterException(String message) {
        super(message, null, false, false);
    }
}
