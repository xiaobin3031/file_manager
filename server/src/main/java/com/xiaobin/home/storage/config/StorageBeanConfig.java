package com.xiaobin.home.storage.config;

import com.xiaobin.home.storage.LocalStorageService;
import com.xiaobin.home.storage.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
public class StorageBeanConfig {

    @Primary
    @Bean
    public StorageService storageService() {
        return new LocalStorageService();
    }
}
