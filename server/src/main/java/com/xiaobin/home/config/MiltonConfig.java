package com.xiaobin.home.config;

import com.xiaobin.home.constant.Constant;
import com.xiaobin.home.webDav.WebDavResourceFactory;
import com.xiaobin.home.webDav.WebDavService;
import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.http.SecurityManager;
import io.milton.http.fs.NullSecurityManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class MiltonConfig {

    @Bean
    public SecurityManager securityManager() {
        return new NullSecurityManager();
    }

    @Bean
    public HttpManager httpManager(ResourceFactory resourceFactory, SecurityManager securityManager) {
        HttpManagerBuilder builder = new HttpManagerBuilder();
        builder.setSecurityManager(securityManager);
        builder.setResourceFactory(resourceFactory);
        return builder.buildHttpManager();
    }

    @Bean
    public ResourceFactory resourceFactory(WebDavService service) {
        return new WebDavResourceFactory(service);
    }

    @Bean
    public FilterRegistrationBean<WebDavFilter> webDavFilterBean(WebDavFilter filter) {
        FilterRegistrationBean<WebDavFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns(Constant.WEBDAV_PATH + "*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
