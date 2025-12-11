package com.example.smartdoc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 用于注册拦截器、配置跨域、资源映射等
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * 注册拦截器
     * 配置登录拦截器，拦截所有接口，但排除登录和注册接口
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/**") // 拦截所有接口
                .excludePathPatterns("/api/user/login", "/api/user/register"); // 排除登录注册接口
    }
}