package com.xiaobin.home.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("login")
public class LoginConfig {

    private int maxAliveMinutes = 5;
}
