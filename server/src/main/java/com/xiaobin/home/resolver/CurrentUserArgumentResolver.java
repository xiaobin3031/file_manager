package com.xiaobin.home.resolver;

import com.xiaobin.home.annotation.CurrentUser;
import com.xiaobin.home.dto.login.LoginUser;
import com.xiaobin.home.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private LoginService loginService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 参数必须带有 @CurrentUser 注解，且类型为 User
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                LoginUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        // 假设你有一个 JwtUtils 工具类
        return loginService.getLoginUser();  // 返回 User 对象
    }
}
