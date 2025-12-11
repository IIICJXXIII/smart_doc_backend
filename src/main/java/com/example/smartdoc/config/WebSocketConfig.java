package com.example.smartdoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类
 * 开启 WebSocket 支持
 */
@Configuration
public class WebSocketConfig {
    /**
     * 注入 ServerEndpointExporter
     * 会自动扫描并注册所有 @ServerEndpoint 注解的 Bean
     *
     * @return ServerEndpointExporter 实例
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}