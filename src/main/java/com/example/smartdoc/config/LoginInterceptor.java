package com.example.smartdoc.config;

import com.example.smartdoc.controller.UserController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 用于校验请求头中的 Token 是否有效
 * 如果无效则拦截请求并返回 401
 */
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 请求处理前调用 (Controller 方法执行之前)
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  被调用的处理器
     * @return true=放行, false=拦截
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 放行 OPTIONS 请求 (浏览器跨域预检)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 从请求头获取 Token
        String token = request.getHeader("Authorization");

        // 2. 检查 Token 是否有效
        if (token != null && UserController.tokenMap.containsKey(token)) {
            return true; // 验证通过，放行
        }

        // 3. 验证失败，返回 401 状态码
        response.setStatus(401);
        return false;
    }
}