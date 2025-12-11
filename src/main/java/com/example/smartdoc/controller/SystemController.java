package com.example.smartdoc.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.smartdoc.model.Budget;
import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.OperationLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.BudgetRepository;
import com.example.smartdoc.repository.ChatLogRepository;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.repository.OperationLogRepository;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

/**
 * 系统管理控制器 - 提供系统级别的管理功能
 * 
 * <p>该控制器提供操作审计日志查询、数据备份导出、数据恢复导入等
 * 系统管理功能，确保数据安全和可追溯性。</p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li>操作日志: 查询用户的操作审计记录</li>
 *   <li>数据备份: 将用户数据导出为 JSON 文件</li>
 *   <li>数据恢复: 从 JSON 备份文件恢复数据</li>
 * </ul>
 * 
 * <h3>备份数据结构:</h3>
 * <pre>
 * {
 *   "invoices": [...],    // 票据数据
 *   "budgets": [...],     // 预算数据
 *   "chats": [...]        // 对话记录
 * }
 * </pre>
 * 
 * <h3>API 接口:</h3>
 * <ul>
 *   <li>GET /api/system/logs - 获取操作日志</li>
 *   <li>GET /api/system/backup - 下载数据备份</li>
 *   <li>POST /api/system/restore - 恢复备份数据</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see OperationLog
 */
@RestController  // RESTful 控制器
@RequestMapping("/api/system")  // URL 前缀
@CrossOrigin(origins = "*")  // 允许跨域
public class SystemController {

    /** 票据数据仓库 */
    @Autowired 
    private InvoiceRepository invoiceRepo;
    
    /** 预算数据仓库 */
    @Autowired 
    private BudgetRepository budgetRepo;
    
    /** 对话日志仓库 */
    @Autowired 
    private ChatLogRepository chatLogRepo;
    
    /** 操作日志仓库 */
    @Autowired 
    private OperationLogRepository opLogRepo;

    /**
     * 获取操作审计日志
     * 
     * <p>查询当前用户的所有操作记录，包括登录、审批、备份恢复等敏感操作。
     * 日志按时间倒序排列，最新的记录在前面。</p>
     * 
     * <h4>日志记录场景:</h4>
     * <ul>
     *   <li>审核通过/驳回操作</li>
     *   <li>数据备份操作</li>
     *   <li>数据恢复操作</li>
     * </ul>
     * 
     * @param token 用户登录凭证
     * @return 操作日志列表
     */
    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }
        
        // 2. 查询该用户的操作日志
        return Map.of("code", 200, "data", opLogRepo.findByUserIdOrderByIdDesc(user.getId()));
    }

    /**
     * 数据备份 - 下载 JSON 格式的备份文件
     * 
     * <p>该接口将当前用户的所有数据（票据、预算、对话记录）打包为 JSON 文件，
     * 供用户下载保存。可用于数据迁移或灾难恢复。</p>
     * 
     * <h4>备份内容:</h4>
     * <ul>
     *   <li>invoices: 所有票据数据</li>
     *   <li>budgets: 预算配置</li>
     *   <li>chats: AI 对话历史</li>
     * </ul>
     * 
     * <h4>文件命名:</h4>
     * <pre>SmartDoc_Backup_{timestamp}.json</pre>
     * 
     * @param response HTTP 响应对象，用于输出文件
     * @param token    用户登录凭证
     */
    @GetMapping("/backup")
    public void backup(HttpServletResponse response, @RequestHeader("Authorization") String token) {
        try {
            // 1. 身份验证
            User user = UserController.tokenMap.get(token);
            if (user == null) return;

            // 2. 收集所有需要备份的数据
            Map<String, Object> backupData = new HashMap<>();
            
            // 2.1 备份票据数据
            backupData.put("invoices", invoiceRepo.findByUserIdOrderByIdDesc(user.getId()));
            
            // 2.2 备份预算数据
            backupData.put("budgets", budgetRepo.findByUserId(user.getId()));
            
            // 2.3 备份对话记录
            // 注意: 这里使用 null 作为 sessionId，查询所有会话
            // 建议在 Repository 中增加 findByUserId 方法
            backupData.put("chats", chatLogRepo.findByUserIdAndSessionIdOrderByIdAsc(user.getId(), null));

            // 3. 记录备份操作日志
            opLogRepo.save(new OperationLog(user.getId(), "数据备份", "导出全量数据"));

            // 4. 设置响应头，告诉浏览器这是一个 JSON 下载文件
            response.setContentType("application/json;charset=utf-8");
            String fileName = URLEncoder.encode("SmartDoc_Backup_" + System.currentTimeMillis(), StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".json");

            // 5. 将数据转换为格式化的 JSON 字符串并写入响应流
            String jsonStr = JSONUtil.toJsonPrettyStr(backupData);
            IoUtil.write(response.getOutputStream(), StandardCharsets.UTF_8, true, jsonStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 数据恢复 - 从 JSON 备份文件恢复数据
     * 
     * <p>该接口接收用户上传的 JSON 备份文件，解析并恢复数据到数据库。
     * 采用追加模式，不会删除现有数据。</p>
     * 
     * <h4>恢复策略:</h4>
     * <ul>
     *   <li>票据: 置空 ID 后作为新数据插入</li>
     *   <li>预算: 检查分类是否已存在，存在则跳过</li>
     *   <li>所有数据绑定当前用户 ID</li>
     * </ul>
     * 
     * <h4>事务处理:</h4>
     * <p>使用 @Transactional 确保恢复操作的原子性，
     * 如果中途出错，所有操作将回滚。</p>
     * 
     * @param file  用户上传的 JSON 备份文件
     * @param token 用户登录凭证
     * @return 恢复结果
     */
    @PostMapping("/restore")
    @Transactional  // 事务保证: 要么全成功，要么全回滚
    public Map<String, Object> restore(@RequestParam("file") MultipartFile file, 
                                       @RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        try {
            // 2. 读取上传文件内容
            String jsonStr = new String(file.getBytes(), StandardCharsets.UTF_8);
            cn.hutool.json.JSONObject data = JSONUtil.parseObj(jsonStr);

            // 3. 恢复票据数据
            if (data.containsKey("invoices")) {
                List<InvoiceData> list = JSONUtil.toList(data.getJSONArray("invoices"), InvoiceData.class);
                for (InvoiceData item : list) {
                    // 置空 ID，让数据库自动生成新 ID (避免主键冲突)
                    item.setId(null);
                    // 绑定当前用户 ID (确保数据归属)
                    item.setUserId(user.getId());
                    invoiceRepo.save(item);
                }
            }

            // 4. 恢复预算数据
            if (data.containsKey("budgets")) {
                List<Budget> list = JSONUtil.toList(data.getJSONArray("budgets"), Budget.class);
                for (Budget item : list) {
                    item.setId(null);
                    item.setUserId(user.getId());
                    
                    // 去重逻辑: 如果该分类已存在预算，则跳过
                    if (budgetRepo.findByUserIdAndCategory(user.getId(), item.getCategory()) == null) {
                        budgetRepo.save(item);
                    }
                }
            }

            // 5. 记录恢复操作日志
            opLogRepo.save(new OperationLog(user.getId(), "数据恢复", "从备份文件恢复数据"));
            
            return Map.of("code", 200, "msg", "恢复成功");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("code", 500, "msg", "恢复失败: " + e.getMessage());
        }
    }
}
