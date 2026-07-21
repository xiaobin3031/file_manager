package com.xiaobin.home.node.config;

import com.xiaobin.home.node.LocalNodeService;
import com.xiaobin.home.node.NodeService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class NodeBeanConfig {

    @Bean
    public NodeService nodeService() {
        return new LocalNodeService();
    }
}
