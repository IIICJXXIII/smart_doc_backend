package com.example.smartdoc.controller;

import cn.hutool.http.HttpRequest;
import com.example.smartdoc.model.ChatLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.ChatLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/chat/{token}")
@Component
public class ChatServer {

    private static String apiKey;
    private static String apiUrl;

    // 注入 JDBC 模板，用于执行原生 SQL
    private static JdbcTemplate jdbcTemplate;
    private static ChatLogRepository chatLogRepository;

    private static CopyOnWriteArraySet<ChatServer> webSocketSet = new CopyOnWriteArraySet<>();

    private Session session;
    private Long currentUserId;

    @Component
    public static class ChatConfig {
        @Value("${deepseek.api.key}")
        private String key;
        @Value("${deepseek.api.url}")
        private String url;
        @Autowired
        private ChatLogRepository chatLogRepo;
        @Autowired
        private JdbcTemplate jdbc; // 注入 JDBC

        @PostConstruct
        public void init() {
            ChatServer.apiKey = key;
            ChatServer.apiUrl = url;
            ChatServer.chatLogRepository = chatLogRepo;
            ChatServer.jdbcTemplate = jdbc;
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;
        User user = UserController.tokenMap.get(token);
        if (user != null) {
            this.currentUserId = user.getId();
            webSocketSet.add(this);
            System.out.println("✅ 用户 " + user.getUsername() + " 连接 WebSocket");
        } else {
            try { session.close(); } catch (IOException e) {}
        }
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
    }

    @OnMessage
    public void onMessage(String messageJson, Session session) {
        // 0. 安全校验
        if (this.currentUserId == null) return;

        try {
            // 1. 解析前端发来的 JSON (格式: { "sessionId": "...", "content": "..." })
            JSONObject msgObj = new JSONObject(messageJson);
            String sessionId = msgObj.optString("sessionId", "default");
            String userContent = msgObj.getString("content");

            // 2. 保存用户提问到数据库 (带上 sessionId)
            saveLog("user", userContent, sessionId);

            // 3. 进入 Agent 流程
            String finalAnswer;

            // 第一步：让 AI 基于当前时间 + 数据库结构 + 用户问题，尝试生成 SQL
            String sqlOrResponse = generateSqlFromAI(userContent);

            // 判断 AI 是否返回了 SQL (通过前缀判断)
            if (sqlOrResponse.trim().toUpperCase().startsWith("SELECT")) {
                // (可选) 给用户一个中间反馈，但这句不会存库，只用于即时展示
                sendMessage("🔍 正在查询数据库...");

                // 执行 SQL (带容错机制)
                String queryResult = executeSqlSafe(sqlOrResponse);

                // 第二步：让 AI 根据查询结果生成最终的人话回答
                finalAnswer = summarizeDataWithAI(userContent, queryResult);
            } else {
                // 如果 AI 觉得不需要查库（比如用户只是打招呼），直接使用 AI 的回复
                finalAnswer = sqlOrResponse;
            }

            // 4. 发送最终结果给前端
            sendMessage(finalAnswer);

            // 5. 保存 AI 回答到数据库 (带上 sessionId)
            saveLog("ai", finalAnswer, sessionId);

        } catch (Exception e) {
            e.printStackTrace();
            // 发生异常时通知前端
            try {
                sendMessage("系统繁忙: " + e.getMessage());
            } catch (IOException ex) {}
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    // --- 核心方法 1: 让 AI 写 SQL  ---
    private String generateSqlFromAI(String userMessage) {
        // 1. 获取当前真实日期 (关键步骤！)
        String todayDate = java.time.LocalDate.now().toString(); // e.g. "2025-12-10"

        String validCategories = "['餐饮美食', '交通出行', '办公耗材', '通讯网络', '电子设备', '其他']";

        // 2. 定义数据库结构
        String tableSchema = String.format("""
            【数据库Schema】：
            表名: invoice_record
            字段:
            - id (INT): 主键
            - user_id (INT): 用户ID (当前用户ID为 %d)
            - merchant_name (VARCHAR): 商户名称
            - item_name (VARCHAR): 项目名称
            - amount (DOUBLE): 金额
            - date (VARCHAR): 日期 (格式 'YYYY-MM-DD')
            - category (VARCHAR): 分类 (可选值: %s)
            """, currentUserId, validCategories);

        // 3. 构建 System Prompt (注入时间 + 增强规则)
        String systemPrompt = String.format("""
            你是一个 MySQL 专家。
            
            【重要上下文】：
            **今天是：%s** (请根据此日期推算相对时间)
            - 如果用户问"本月/这个月"，请匹配 date LIKE 'YYYY-MM-%%' (使用当前月份)
            - 如果用户问"上个月"，请自行推算上个月份
            - 如果用户问"今年"，请匹配 date LIKE 'YYYY-%%'
            
            %s
            
            【思维链与规则】：
            1. **语义映射**：用户用简称时(如"吃饭")，请映射到最接近的 category。
            2. **模糊查询**：商户或项目名请务必使用 LIKE。
            3. **安全限制**：必须在 WHERE 子句中包含 user_id = %d。
            4. **输出格式**：只返回 SQL 语句本身，不要 Markdown，不要解释。
            """, todayDate, tableSchema, currentUserId);

        return callDeepSeekApi(systemPrompt, userMessage);
    }

    // --- 核心方法 2: 执行 SQL ---
    private String executeSqlSafe(String sql) {
        try {
            System.out.println("🤖 AI生成的SQL: " + sql);

            // 安全检查
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                return "错误：AI 生成了非查询语句，已被拦截。";
            }
            if (sql.contains(";")) {
                // 简单的防注入，防止多条语句执行
                sql = sql.split(";")[0];
            }

            // 执行查询
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql);

            if (resultList.isEmpty()) {
                return "查询结果为空 (0条记录)。";
            }

            // 将结果转为 JSON 字符串
            return new JSONArray(resultList).toString();

        } catch (Exception e) {
            return "SQL执行出错: " + e.getMessage();
        }
    }

    // --- 核心方法 3: 让 AI 总结数据 ---
    private String summarizeDataWithAI(String userQuestion, String dataContext) {
        String systemPrompt = """
            你是一个财务数据分析师。
            用户问了一个问题，系统执行 SQL 后得到了以下 JSON 数据。
            请根据这些数据，用简洁、专业的语言回答用户的问题。
            如果数据量很大，只总结关键趋势或总数。
            """;

        String userPrompt = String.format("用户问题：%s\n数据库返回结果：%s", userQuestion, dataContext);

        return callDeepSeekApi(systemPrompt, userPrompt);
    }

    // --- 通用 API 调用方法 ---
    private String callDeepSeekApi(String systemPrompt, String userMsg) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.1); // 这里设低一点，让写 SQL 更严谨

            JSONArray messages = new JSONArray();
            JSONObject sysObj = new JSONObject().put("role", "system").put("content", systemPrompt);
            JSONObject userObj = new JSONObject().put("role", "user").put("content", userMsg);
            messages.put(sysObj).put(userObj);
            body.put("messages", messages);

            String response = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(30000)
                    .execute()
                    .body();

            JSONObject jsonResponse = new JSONObject(response);
            if (jsonResponse.has("error")) return "Error: " + jsonResponse.get("error");

            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            return "API调用失败: " + e.getMessage();
        }
    }

    // 修改后的 saveLog 方法：接收 3 个参数
    private void saveLog(String role, String content, String sessionId) {
        try {
            ChatLog log = new ChatLog();
            log.setUserId(this.currentUserId);
            log.setRole(role);
            log.setContent(content);
            log.setSessionId(sessionId); // 关键：保存会话ID
            chatLogRepository.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}