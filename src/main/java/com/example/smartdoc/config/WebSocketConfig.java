package com.example.smartdoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置类 - 启用 WebSocket 服务端功能
 * 
 * <p>该配置类负责向 Spring 容器注册 WebSocket 所需的组件，
 * 使应用程序能够处理 WebSocket 连接和消息通信。</p>
 * 
 * <h3>WebSocket 在本项目中的应用:</h3>
 * <ul>
 *   <li>AI 智能财务助手: 用户与 DeepSeek 大模型的实时对话</li>
 *   <li>流式响应: AI 回复可以逐步推送给前端，提升用户体验</li>
 *   <li>会话保持: 支持长连接，避免频繁建立 HTTP 连接</li>
 * </ul>
 * 
 * <h3>连接地址:</h3>
 * <pre>ws://localhost:8080/ws/chat/{token}</pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.ChatServer
 */
@Configuration  // 标记为 Spring 配置类
public class WebSocketConfig {
    
    /**
     * 注册 ServerEndpointExporter Bean
     * 
     * <p>该 Bean 是 Spring 整合 JSR-356 (Java WebSocket API) 的关键组件。
     * 它会自动扫描容器中标注了 {@code @ServerEndpoint} 注解的类，
     * 并将其注册为 WebSocket 端点。</p>
     * 
     * <h4>工作原理:</h4>
     * <ol>
     *   <li>Spring 启动时创建此 Bean</li>
     *   <li>该 Bean 扫描所有标注了 @ServerEndpoint 的类 (如 ChatServer)</li>
     *   <li>将这些类注册到 WebSocket 服务器</li>
     *   <li>客户端即可通过 ws:// 协议连接到相应端点</li>
     * </ol>
     * 
     * <h4>注意事项:</h4>
     * <p>该 Bean 仅在使用内嵌 Tomcat/Jetty 时需要。
     * 如果部署到外部容器，则由容器负责管理 WebSocket，此 Bean 可省略。</p>
     * 
     * @return ServerEndpointExporter 实例
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}