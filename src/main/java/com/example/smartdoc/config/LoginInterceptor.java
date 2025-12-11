package com.example.smartdoc.config;

import org.springframework.web.servlet.HandlerInterceptor;

import com.example.smartdoc.controller.UserController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录拦截器 - 用于验证用户身份的安全过滤组件
 * 
 * <p>该拦截器实现了 Spring MVC 的 {@link HandlerInterceptor} 接口，
 * 在请求到达 Controller 之前进行身份验证，确保只有已登录的用户才能访问受保护的接口。</p>
 * 
 * <h3>工作原理:</h3>
 * <ol>
 *   <li>用户登录成功后，系统生成唯一的 Token 并存入 {@link UserController#tokenMap}</li>
 *   <li>后续请求需在 HTTP Header 中携带 Authorization: &lt;token&gt;</li>
 *   <li>拦截器验证该 Token 是否存在于 tokenMap 中</li>
 *   <li>验证通过则放行，失败则返回 401 Unauthorized</li>
 * </ol>
 * 
 * <h3>跨域支持:</h3>
 * <p>自动放行 OPTIONS 预检请求，支持 CORS 跨域场景</p>
 * 
 * @author SmartDoc Team
 * @see HandlerInterceptor
 * @see UserController#tokenMap
 */
public class LoginInterceptor implements HandlerInterceptor {
    
    /**
     * 请求预处理方法 - 在 Controller 方法执行之前调用
     * 
     * <p>该方法负责检查请求中是否携带有效的登录凭证 (Token)，
     * 并决定是否允许该请求继续执行。</p>
     * 
     * @param request  HTTP 请求对象，用于获取请求头、请求方法等信息
     * @param response HTTP 响应对象，用于设置响应状态码
     * @param handler  即将执行的处理器对象 (通常是 Controller 方法)
     * @return true - 放行请求，继续执行 Controller 方法;
     *         false - 拦截请求，不再执行后续处理
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // ========== 1. 处理 CORS 跨域预检请求 ==========
        // 浏览器在发送跨域请求前会先发送 OPTIONS 预检请求
        // 该请求不携带 Token，需要无条件放行
        if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;  // 放行 OPTIONS 请求
        }

        // ========== 2. 从请求头获取 Token ==========
        // 前端需要在请求头中添加: Authorization: <token值>
        String token = request.getHeader("Authorization");

        // ========== 3. 验证 Token 有效性 ==========
        // 检查 Token 是否存在于内存 Map 中 (实际项目建议使用 Redis)
        if (token != null && UserController.tokenMap.containsKey(token)) {
            return true;  // Token 有效，验证通过，放行请求
        }

        // ========== 4. 验证失败处理 ==========
        // Token 无效或未提供，返回 401 Unauthorized 状态码
        // 前端收到 401 后应跳转到登录页面
        response.setStatus(401);
        return false;  // 拦截请求，不执行 Controller 方法
    }
}