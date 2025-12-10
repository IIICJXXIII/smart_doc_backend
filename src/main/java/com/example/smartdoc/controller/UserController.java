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

    // 1. 获取会话列表 (左侧侧边栏用)
    @GetMapping("/chat/sessions")
    public Map<String, Object> getSessions(@RequestHeader("Authorization") String token) {
        User user = tokenMap.get(token);
        if (user == null) return Map.of("code", 401);

        List<String> sessions = chatLogRepository.findSessionIdsByUserId(user.getId());
        return Map.of("code", 200, "data", sessions);
    }

    // 2. 获取某个会话的详细记录 (点击侧边栏时用)
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

    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String token) {
        tokenMap.remove(token);
        return "success";
    }

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
}
