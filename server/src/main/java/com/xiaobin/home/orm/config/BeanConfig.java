package com.xiaobin.home.orm.config;

import com.xiaobin.home.orm.entity.EntityMapper;
import com.xiaobin.home.orm.entity.ReflectionEntityMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BeanConfig {

    @Bean("entityMapper")
    public EntityMapper entityMapper() {
        return new ReflectionEntityMapper();
    }

}
