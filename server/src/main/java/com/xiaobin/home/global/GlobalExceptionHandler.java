package com.xiaobin.home.global;

import com.xiaobin.home.dto.ApiResponse;
import com.xiaobin.home.exception.BizException;
import com.xiaobin.home.exception.SimpleBizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理自定义业务异常
    @ExceptionHandler(BizException.class)
    public ApiResponse handleBizException(BizException e) {
        log.error("biz exception: {}", e.toString(), e);
        return new ApiResponse(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(SimpleBizException.class)
    public ApiResponse handleSimpleBizException(SimpleBizException e) {
        log.error("biz exception: {}", e.toString());
        return new ApiResponse(e.getCode(), e.getMessage(), null);
    }

    // 兜底异常
    @ExceptionHandler(Exception.class)
    public ApiResponse handleException(Exception e) {
        if(!(e instanceof NoResourceFoundException)) {
            log.error("unhand exception: {}", e.getMessage(), e);
        }
        return ApiResponse.error(StringUtils.isEmpty(e.getMessage()) ? "服务器异常，请联系管理员" : e.getMessage());
    }
}
