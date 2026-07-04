package com.xiaobin.home.config;

import com.xiaobin.home.constant.Constant;
import io.milton.http.HttpManager;
import io.milton.servlet.ServletRequest;
import io.milton.servlet.ServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class WebDavFilter extends OncePerRequestFilter {

    private final HttpManager httpManager;

    public WebDavFilter(HttpManager httpManager) {
        this.httpManager = httpManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(Constant.WEBDAV_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }
        ServletRequest miltonRequest = new ServletRequest(request, null);
        ServletResponse miltonResponse = new ServletResponse(response);
        this.httpManager.process(miltonRequest, miltonResponse);
    }
}
