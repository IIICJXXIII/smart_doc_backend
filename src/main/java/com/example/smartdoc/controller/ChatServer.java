package com.example.smartdoc.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.smartdoc.model.ChatLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.ChatLogRepository;

import cn.hutool.http.HttpRequest;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket AI 对话服务器 - 实现智能财务助手功能
 * 
 * <p>该类是整个 AI 对话系统的核心，采用 WebSocket 协议与前端建立实时双向通信，
 * 集成 DeepSeek 大模型实现 Text2SQL 和自然语言财务分析功能。</p>
 * 
 * <h3>核心技术架构:</h3>
 * <pre>
 * 前端 <--WebSocket--> ChatServer <--HTTP--> DeepSeek API
 *                          |
 *                          +--> JdbcTemplate --> MySQL
 * </pre>
 * 
 * <h3>Agent 工作流程 (Text2SQL):</h3>
 * <pre>
 * 1. 用户发送问题: "这个月餐饮花了多少钱？"
 *                    ↓
 * 2. AI 生成 SQL: SELECT SUM(amount) FROM invoice_record 
 *                 WHERE user_id=1 AND category='餐饮美食' 
 *                 AND date LIKE '2025-12%'
 *                    ↓
 * 3. 执行 SQL 获取结果: [{"SUM(amount)": 1523.50}]
 *                    ↓
 * 4. AI 生成自然语言回答: "本月您的餐饮消费共计 1523.50 元"
 * </pre>
 * 
 * <h3>连接地址:</h3>
 * <pre>ws://localhost:8080/ws/chat/{token}</pre>
 * 
 * <h3>消息格式:</h3>
 * <pre>
 * 发送: {"sessionId": "会话ID", "content": "用户问题"}
 * 接收: 纯文本 AI 回答
 * </pre>
 * 
 * @author SmartDoc Team
 * @see jakarta.websocket.server.ServerEndpoint
 */
@ServerEndpoint("/ws/chat/{token}")  // 定义 WebSocket 端点路径，支持 Token 作为路径参数
@Component  // 注册为 Spring 组件
public class ChatServer {

    // ==================== 静态配置 (通过内部类注入) ====================
    
    /** DeepSeek API 密钥 */
    private static String apiKey;
    
    /** DeepSeek API 地址 */
    private static String apiUrl;

    /** JDBC 模板 - 用于执行原生 SQL 查询 */
    private static JdbcTemplate jdbcTemplate;
    
    /** 对话日志仓库 - 用于持久化对话记录 */
    private static ChatLogRepository chatLogRepository;

    /** 
     * 所有活跃的 WebSocket 连接集合
     * 使用 CopyOnWriteArraySet 保证线程安全
     */
    private static CopyOnWriteArraySet<ChatServer> webSocketSet = new CopyOnWriteArraySet<>();

    // ==================== 实例变量 (每个连接独立) ====================
    
    /** 当前 WebSocket 会话对象 */
    private Session session;
    
    /** 当前连接对应的用户 ID */
    private Long currentUserId;

    /**
     * 内部配置类 - 解决 WebSocket 端点无法直接注入 Spring Bean 的问题
     * 
     * <p>由于 {@code @ServerEndpoint} 标注的类由 WebSocket 容器管理，
     * 而非 Spring 容器，因此无法使用 {@code @Autowired} 直接注入依赖。
     * 这里通过静态内部类 + {@code @PostConstruct} 将配置注入到静态变量中。</p>
     */
    @Component
    public static class ChatConfig {
        @Value("${deepseek.api.key}")
        private String key;
        
        @Value("${deepseek.api.url}")
        private String url;
        
        @Autowired
        private ChatLogRepository chatLogRepo;
        
        @Autowired
        private JdbcTemplate jdbc;

        /**
         * Spring 初始化完成后执行，将依赖注入到外部类的静态变量
         */
        @PostConstruct
        public void init() {
            ChatServer.apiKey = key;
            ChatServer.apiUrl = url;
            ChatServer.chatLogRepository = chatLogRepo;
            ChatServer.jdbcTemplate = jdbc;
        }
    }

    // ==================== WebSocket 生命周期回调 ====================

    /**
     * WebSocket 连接建立时回调
     * 
     * <p>客户端发起 WebSocket 连接时触发此方法，
     * 负责验证 Token 有效性并初始化连接状态。</p>
     * 
     * @param session WebSocket 会话对象，用于发送消息
     * @param token   用户登录凭证，从 URL 路径中提取
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        this.session = session;
        
        // 根据 Token 获取用户信息 (复用 HTTP 登录产生的 tokenMap)
        User user = UserController.tokenMap.get(token);
        
        if (user != null) {
            // Token 有效: 记录用户 ID，将此连接加入活跃集合
            this.currentUserId = user.getId();
            webSocketSet.add(this);
            System.out.println("✅ 用户 " + user.getUsername() + " 连接 WebSocket");
        } else {
            // Token 无效: 拒绝连接，关闭会话
            try { 
                session.close(); 
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * WebSocket 连接关闭时回调
     * 
     * <p>客户端断开连接或服务端主动关闭时触发，
     * 负责从活跃连接集合中移除当前连接。</p>
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
    }

    /**
     * 收到客户端消息时回调 - AI 对话核心入口
     * 
     * <p>该方法实现了完整的 Text2SQL Agent 流程，
     * 根据用户问题智能决定是否需要查询数据库。</p>
     * 
     * <h4>处理流程:</h4>
     * <ol>
     *   <li>解析消息 JSON，提取 sessionId 和 content</li>
     *   <li>保存用户问题到数据库</li>
     *   <li>调用 AI 生成 SQL (或直接回答)</li>
     *   <li>如果是 SQL，执行查询获取结果</li>
     *   <li>让 AI 根据查询结果生成自然语言回答</li>
     *   <li>发送回答给客户端并保存到数据库</li>
     * </ol>
     * 
     * @param messageJson 客户端发送的 JSON 格式消息
     * @param session     WebSocket 会话对象
     */
    @OnMessage
    public void onMessage(String messageJson, Session session) {
        // 安全校验: 未认证用户不处理消息
        if (this.currentUserId == null) return;

        try {
            // ========== 1. 解析前端消息 ==========
            // 消息格式: { "sessionId": "xxx", "content": "用户问题" }
            JSONObject msgObj = new JSONObject(messageJson);
            String sessionId = msgObj.optString("sessionId", "default");  // 会话 ID，支持多会话
            String userContent = msgObj.getString("content");  // 用户实际问题

            // ========== 2. 保存用户问题到数据库 ==========
            saveLog("user", userContent, sessionId);

            // ========== 3. 进入 Agent 流程 ==========
            String finalAnswer;

            // 第一步: 让 AI 基于当前时间 + 数据库结构 + 用户问题，尝试生成 SQL
            // 如果 AI 认为不需要查库（如用户只是打招呼），会直接返回自然语言回答
            String sqlOrResponse = generateSqlFromAI(userContent);

            // ========== 4. 判断 AI 返回是否为 SQL ==========
            if (sqlOrResponse.trim().toUpperCase().startsWith("SELECT")) {
                // 返回的是 SQL 语句，需要执行查询
                
                // 给用户一个中间反馈 (即时展示，不存库)
                sendMessage("🔍 正在查询数据库...");

                // 执行 SQL (带容错机制，防止 AI 生成错误的 SQL)
                String queryResult = executeSqlSafe(sqlOrResponse);

                // 第二步: 让 AI 根据查询结果生成最终的人话回答
                finalAnswer = summarizeDataWithAI(userContent, queryResult);
            } else {
                // AI 认为不需要查库，直接使用返回的自然语言回答
                finalAnswer = sqlOrResponse;
            }

            // ========== 5. 发送最终结果给前端 ==========
            sendMessage(finalAnswer);

            // ========== 6. 保存 AI 回答到数据库 ==========
            saveLog("ai", finalAnswer, sessionId);

        } catch (Exception e) {
            e.printStackTrace();
            // 发生异常时通知前端
            try {
                sendMessage("系统繁忙: " + e.getMessage());
            } catch (IOException ex) {
                // 忽略发送异常
            }
        }
    }

    /**
     * WebSocket 发生错误时回调
     * 
     * @param session 发生错误的会话
     * @param error   错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    /**
     * 向当前客户端发送文本消息
     * 
     * @param message 要发送的消息内容
     * @throws IOException 发送失败时抛出
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    // ==================== AI 核心方法 ====================

    /**
     * 核心方法 1: 让 AI 生成 SQL 查询语句
     * 
     * <p>该方法构建精心设计的 System Prompt，引导 AI 根据用户问题生成正确的 SQL。
     * Prompt 中注入了当前日期、数据库结构、用户 ID 等关键上下文。</p>
     * 
     * <h4>Prompt 工程要点:</h4>
     * <ul>
     *   <li>注入当前日期，让 AI 理解"本月"、"上个月"等相对时间</li>
     *   <li>明确数据库表结构和字段类型</li>
     *   <li>强制 SQL 包含 user_id 条件，防止数据泄露</li>
     *   <li>使用低温度 (0.1) 确保输出稳定</li>
     * </ul>
     * 
     * @param userMessage 用户的原始问题
     * @return SQL 语句 或 直接的自然语言回答
     */
    private String generateSqlFromAI(String userMessage) {
        // 1. 获取当前真实日期 (关键！让 AI 理解相对时间)
        String todayDate = java.time.LocalDate.now().toString();  // e.g. "2025-12-10"

        // 分类的合法值列表
        String validCategories = "['餐饮美食', '交通出行', '办公耗材', '通讯网络', '电子设备', '其他']";

        // 2. 定义数据库 Schema (让 AI 理解表结构)
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

        // 3. 构建 System Prompt (核心 Prompt 工程)
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

        // 4. 调用 DeepSeek API
        return callDeepSeekApi(systemPrompt, userMessage);
    }

    /**
     * 核心方法 2: 安全执行 SQL 查询
     * 
     * <p>该方法对 AI 生成的 SQL 进行安全检查和执行，
     * 包含多重防护机制防止 SQL 注入等安全问题。</p>
     * 
     * <h4>安全措施:</h4>
     * <ul>
     *   <li>只允许执行 SELECT 语句</li>
     *   <li>过滤分号防止多语句注入</li>
     *   <li>异常捕获并返回友好错误信息</li>
     * </ul>
     * 
     * @param sql AI 生成的 SQL 语句
     * @return 查询结果 (JSON 字符串) 或 错误信息
     */
    private String executeSqlSafe(String sql) {
        try {
            System.out.println("🤖 AI生成的SQL: " + sql);

            // 安全检查 1: 只允许 SELECT 语句
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                return "错误：AI 生成了非查询语句，已被拦截。";
            }
            
            // 安全检查 2: 防止多语句注入，只取分号前的部分
            if (sql.contains(";")) {
                sql = sql.split(";")[0];
            }

            // 执行查询
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql);

            // 处理空结果
            if (resultList.isEmpty()) {
                return "查询结果为空 (0条记录)。";
            }

            // 将结果转为 JSON 字符串，供 AI 分析
            return new JSONArray(resultList).toString();

        } catch (Exception e) {
            return "SQL执行出错: " + e.getMessage();
        }
    }

    /**
     * 核心方法 3: 让 AI 根据查询结果生成自然语言回答
     * 
     * <p>该方法接收 SQL 查询结果，让 AI 将其转换为用户友好的自然语言。
     * 特别适合处理复杂的聚合查询结果。</p>
     * 
     * @param userQuestion 用户原始问题 (作为上下文)
     * @param dataContext  SQL 查询结果 (JSON 格式)
     * @return AI 生成的自然语言回答
     */
    private String summarizeDataWithAI(String userQuestion, String dataContext) {
        String systemPrompt = """
            你是一个财务数据分析师。
            用户问了一个问题，系统执行 SQL 后得到了以下 JSON 数据。
            请根据这些数据，用简洁、专业的语言回答用户的问题。
            如果数据量很大，只总结关键趋势或总数。
            """;

        // 组合用户问题和查询结果作为 User Prompt
        String userPrompt = String.format("用户问题：%s\n数据库返回结果：%s", userQuestion, dataContext);

        return callDeepSeekApi(systemPrompt, userPrompt);
    }

    /**
     * 通用 DeepSeek API 调用方法
     * 
     * <p>封装了 HTTP 请求的细节，包括请求构建、超时设置、响应解析等。
     * 使用较低的温度值 (0.1) 确保 SQL 生成的稳定性。</p>
     * 
     * @param systemPrompt 系统提示词，定义 AI 的角色和行为
     * @param userMsg      用户消息内容
     * @return AI 的回复内容
     */
    private String callDeepSeekApi(String systemPrompt, String userMsg) {
        try {
            // 构建请求体
            JSONObject body = new JSONObject();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.1);  // 低温度，让 SQL 生成更严谨

            // 构建消息数组 (OpenAI 兼容格式)
            JSONArray messages = new JSONArray();
            JSONObject sysObj = new JSONObject().put("role", "system").put("content", systemPrompt);
            JSONObject userObj = new JSONObject().put("role", "user").put("content", userMsg);
            messages.put(sysObj).put(userObj);
            body.put("messages", messages);

            // 发送 HTTP POST 请求
            String response = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(30000)  // 30 秒超时
                    .execute()
                    .body();

            // 解析响应
            JSONObject jsonResponse = new JSONObject(response);
            
            // 检查是否有错误
            if (jsonResponse.has("error")) {
                return "Error: " + jsonResponse.get("error");
            }

            // 提取 AI 回复内容
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                    
        } catch (Exception e) {
            return "API调用失败: " + e.getMessage();
        }
    }

    /**
     * 保存对话记录到数据库
     * 
     * <p>将用户问题和 AI 回答持久化存储，支持多会话管理和历史记录查询。</p>
     * 
     * @param role      角色标识: "user" 表示用户, "ai" 表示 AI
     * @param content   消息内容
     * @param sessionId 会话 ID，用于区分不同的对话
     */
    private void saveLog(String role, String content, String sessionId) {
        try {
            ChatLog log = new ChatLog();
            log.setUserId(this.currentUserId);
            log.setRole(role);
            log.setContent(content);
            log.setSessionId(sessionId);  // 关键: 保存会话 ID，支持多会话
            chatLogRepository.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}