package com.example.smartdoc.config;

import com.example.smartdoc.controller.UserController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求 (浏览器跨域预检)
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
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