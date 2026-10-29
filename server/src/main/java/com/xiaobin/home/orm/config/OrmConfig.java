package com.xiaobin.home.orm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("tiny-orm")
@Configuration
public class OrmConfig {

    private List<String> entityPackages;
}
