package com.xiaobin.home.exception;

public class LoginException extends RuntimeException {

    public LoginException(String message) {
        super(message, null, false, false);
    }
}
