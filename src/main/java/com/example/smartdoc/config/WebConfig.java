package com.example.smartdoc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC Web 配置类 - 用于自定义 MVC 框架行为
 * 
 * <p>该类通过实现 {@link WebMvcConfigurer} 接口来扩展 Spring MVC 的默认配置，
 * 主要用于注册自定义拦截器、跨域配置、视图解析器等。</p>
 * 
 * <h3>当前配置内容:</h3>
 * <ul>
 *   <li>注册登录拦截器 {@link LoginInterceptor}</li>
 *   <li>配置需要拦截的 URL 路径模式</li>
 *   <li>配置排除在拦截之外的 URL (如登录、注册接口)</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see WebMvcConfigurer
 * @see LoginInterceptor
 */
@Configuration  // 标记为 Spring 配置类，启动时自动加载
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 注册拦截器方法 - 配置请求拦截规则
     * 
     * <p>该方法会在 Spring 启动时被调用，用于向拦截器链中添加自定义拦截器。
     * 拦截器会按照添加顺序依次执行。</p>
     * 
     * @param registry 拦截器注册器，用于添加和配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器到拦截器链
        registry.addInterceptor(new LoginInterceptor())
                // 配置需要拦截的路径模式
                // "/api/**" 表示拦截 /api/ 下的所有请求
                // 例如: /api/doc/list, /api/budget/save 等
                .addPathPatterns("/api/**")
                
                // 配置排除在拦截之外的路径 (白名单)
                // 这些路径不需要登录即可访问
                .excludePathPatterns(
                    "/api/user/login",     // 登录接口 - 未登录用户需要访问
                    "/api/user/register"   // 注册接口 - 新用户需要访问
                );
    }
}