package com.xiaobin.home.global;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaobin.home.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

//@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    // 判断是否要拦截（这里对所有返回值都处理）
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 不包装 ResponseEntity 或已经是 ApiResponse 的对象
        return !returnType.getParameterType().equals(ApiResponse.class)
                && !ResponseEntity.class.isAssignableFrom(returnType.getParameterType());
    }

    // 统一封装处理逻辑
    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        // 针对 String 类型特殊处理
        if (body instanceof String) {
            // 手动将包装后的对象序列化为 JSON 字符串返回
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(ApiResponse.ok(body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("处理String类型时转换失败", e);
            }
        }

        return ApiResponse.ok(body);
    }
}
