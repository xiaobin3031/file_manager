package com.xiaobin.home.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse {

    private int code;

    private String msg;

    private Object data;

    public static ApiResponse ok(Object data) {
        return new ApiResponse(0, "success", data);
    }

    public static ApiResponse ok() {
        return new ApiResponse(0, "success", null);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(-1, message, null);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }
}
