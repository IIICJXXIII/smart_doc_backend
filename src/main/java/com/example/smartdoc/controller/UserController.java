package com.example.smartdoc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartdoc.model.ChatLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.UserRepository;

import cn.hutool.core.util.IdUtil;

/**
 * 用户控制器 - 处理用户认证和个人信息管理
 * 
 * <p>该控制器提供用户注册、登录、登出、信息更新等基础功能，
 * 以及 AI 对话会话管理相关接口。采用 Token 机制实现无状态身份认证。</p>
 * 
 * <h3>认证机制:</h3>
 * <pre>
 * 1. 用户登录成功 → 生成 UUID Token
 * 2. Token 与用户信息存入内存 Map (tokenMap)
 * 3. 后续请求携带 Token → 拦截器验证
 * 4. 登出时从 Map 中移除 Token
 * </pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.config.LoginInterceptor
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    /** 用户数据仓库 */
    @Autowired
    private UserRepository userRepository;

    /**
     * Token 到用户对象的映射表 (内存存储)
     * <p>简化的 Session 实现，将 Token 与用户对象关联。
     * 生产环境建议改用 Redis 实现分布式 Session。</p>
     */
    public static Map<String, User> tokenMap = new HashMap<>();

    /** 对话日志仓库 - 用于查询 AI 对话历史 */
    @Autowired
    private com.example.smartdoc.repository.ChatLogRepository chatLogRepository;

    /**
     * 获取 AI 对话会话列表
     * <p>返回当前用户的所有 AI 对话会话 ID 列表，
     * 用于前端侧边栏展示历史对话入口。</p>
     * 
     * @param token 用户登录凭证
     * @return 会话 ID 列表
     */
    @GetMapping("/chat/sessions")
    public Map<String, Object> getSessions(@RequestHeader("Authorization") String token) {
        User user = tokenMap.get(token);
        if (user == null) return Map.of("code", 401);

        List<String> sessions = chatLogRepository.findSessionIdsByUserId(user.getId());
        return Map.of("code", 200, "data", sessions);
    }

    // 2. 获取某个会话的详细记录 (点击侧边栏时用)
    /**
     * 获取指定会话的对话历史
     * <p>返回某个会话中的所有对话记录，按时间正序排列。
     * 用于前端点击侧边栏会话时展示完整对话内容。</p>
     * 
     * @param token     用户登录凭证
     * @param sessionId 会话 ID
     * @return 对话记录列表
     */
    @GetMapping("/chat/history")
    public Map<String, Object> getChatHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam("sessionId") String sessionId // 增加参数
    ) {
        User user = tokenMap.get(token);
        if (user == null) return Map.of("code", 401);

        List<ChatLog> logs = chatLogRepository.findByUserIdAndSessionIdOrderByIdAsc(user.getId(), sessionId);
        return Map.of("code", 200, "data", logs);
    }

    /**
     * 用户登录
     * <p>验证用户名和密码，成功后生成 Token 并返回。
     * Token 是后续所有需要认证接口的"通行证"。</p>
     * 
     * @param loginUser 包含用户名和密码的用户对象
     * @return 登录结果，成功时包含 token 和用户信息
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginUser) {
        Map<String, Object> result = new HashMap<>();

        // 1. 根据用户名查询数据库
        User dbUser = userRepository.findByUsername(loginUser.getUsername());

        // 2. 校验密码
        if (dbUser == null || !dbUser.getPassword().equals(loginUser.getPassword())) {
            result.put("code", 400);
            result.put("msg", "用户名或密码错误");
            return result;
        }

        // 3. 生成 Token (这就是用户的“门票”)
        String token = IdUtil.simpleUUID();
        tokenMap.put(token, dbUser); // 记录门票属于谁

        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("token", token);
        result.put("user", dbUser);
        return result;
    }

    /**
     * 用户登出
     * <p>清除用户的登录状态，使 Token 失效。</p>
     * 
     * @param token 用户登录凭证
     * @return "success" 表示登出成功
     */
    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String token) {
        tokenMap.remove(token);
        return "success";
    }

    /**
     * 用户注册
     * <p>创建新用户账号。注册时会检查用户名是否已被占用，
     * 并自动设置默认昵称和角色。</p>
     * 
     * @param user 新用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查用户名是否存在
        User existUser = userRepository.findByUsername(user.getUsername());
        if (existUser != null) {
            result.put("code", 400);
            result.put("msg", "该用户名已被占用");
            return result;
        }

        // 2. 补全默认信息
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname("新用户" + System.currentTimeMillis() % 1000);
        }
        user.setRole("user"); // 固定角色为普通用户

        // 3. 保存到数据库
        try {
            userRepository.save(user);
            result.put("code", 200);
            result.put("msg", "注册成功，请登录");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "注册失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新用户信息
     * <p>允许用户修改自己的昵称和密码。
     * 更新后会同步刷新内存中的用户信息。</p>
     * 
     * @param user  包含更新字段的用户对象
     * @param token 用户登录凭证
     * @return 更新结果
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody User user, @RequestHeader("Authorization") String token) {
        User currentUser = tokenMap.get(token);
        if (currentUser == null) return Map.of("code", 401);

        // 从数据库重新查询用户信息
        User dbUser = userRepository.findById(currentUser.getId()).orElse(null);
        if (dbUser != null) {
            // 更新非空字段
            if (user.getNickname() != null) dbUser.setNickname(user.getNickname());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                dbUser.setPassword(user.getPassword()); // 实际建议加密
            }
            userRepository.save(dbUser);

            // 同步更新内存中的 tokenMap
            tokenMap.put(token, dbUser);

            return Map.of("code", 200, "msg", "更新成功", "user", dbUser);
        }
        return Map.of("code", 500, "msg", "用户不存在");
    }
}
