package com.example.smartdoc.controller;

import cn.hutool.core.util.IdUtil;
import com.example.smartdoc.model.ChatLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 处理用户登录、注册、注销、个人信息更新以及会话列表查询
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 简单的内存 Token 存储 (实际项目建议用 Redis)
    public static Map<String, User> tokenMap = new HashMap<>();

    @Autowired
    private com.example.smartdoc.repository.ChatLogRepository chatLogRepository;

    /**
     * 获取当前用户的会话列表 (用于侧边栏)
     *
     * @param token 用户认证令牌
     * @return 会话 ID 列表
     */
    // 1. 获取会话列表 (左侧侧边栏用)
    @GetMapping("/chat/sessions")
    public Map<String, Object> getSessions(@RequestHeader("Authorization") String token) {
        User user = tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        List<String> sessions = chatLogRepository.findSessionIdsByUserId(user.getId());
        return Map.of("code", 200, "data", sessions);
    }

    /**
     * 获取指定会话的详细聊天记录
     *
     * @param token     用户认证令牌
     * @param sessionId 会话ID
     * @return 聊天记录列表
     */
    // 2. 获取某个会话的详细记录 (点击侧边栏时用)
    @GetMapping("/chat/history")
    public Map<String, Object> getChatHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam("sessionId") String sessionId // 增加参数
    ) {
        User user = tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        List<ChatLog> logs = chatLogRepository.findByUserIdAndSessionIdOrderByIdAsc(user.getId(), sessionId);
        return Map.of("code", 200, "data", logs);
    }

    /**
     * 用户登录
     * 校验用户名密码，验证通过后生成 Token
     *
     * @param loginUser 登录表单数据
     * @return 登录结果（包含 Token 和用户信息）
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginUser) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询用户
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
     * 用户注销
     * 移除内存中的 Token，使其失效
     *
     * @param token 用户认证令牌
     * @return 操作结果
     */
    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String token) {
        tokenMap.remove(token);
        return "success";
    }

    /**
     * 用户注册
     * 创建新用户，校验用户名唯一性
     *
     * @param user 注册表单数据
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
        user.setRole("user"); // 默认注册为普通用户

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
     * 更新用户信息 (昵称、密码)
     *
     * @param user  包含更新信息的对象
     * @param token 用户认证令牌
     * @return 更新后的用户信息
     */
    // 新增：更新用户信息
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody User user, @RequestHeader("Authorization") String token) {
        User currentUser = tokenMap.get(token);
        if (currentUser == null)
            return Map.of("code", 401);

        // 从数据库重新查，确保安全
        User dbUser = userRepository.findById(currentUser.getId()).orElse(null);
        if (dbUser != null) {
            // 更新非空字段
            if (user.getNickname() != null)
                dbUser.setNickname(user.getNickname());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                dbUser.setPassword(user.getPassword()); // 实际建议加密
            }
            userRepository.save(dbUser);

            // 更新内存中的 tokenMap
            tokenMap.put(token, dbUser);

            return Map.of("code", 200, "msg", "更新成功", "user", dbUser);
        }
        return Map.of("code", 500, "msg", "用户不存在");
    }
}
