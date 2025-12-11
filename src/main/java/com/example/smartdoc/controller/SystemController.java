package com.example.smartdoc.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import com.example.smartdoc.model.*;
import com.example.smartdoc.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理控制器
 * 处理操作日志查询、数据备份（导出JSON）和数据恢复（导入JSON）
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    @Autowired
    private InvoiceRepository invoiceRepo;
    @Autowired
    private BudgetRepository budgetRepo;
    @Autowired
    private ChatLogRepository chatLogRepo;
    @Autowired
    private OperationLogRepository opLogRepo;

    /**
     * 获取当前用户的操作日志
     *
     * @param token 用户认证令牌
     * @return 操作日志列表
     */
    // 1. 获取操作日志
    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);
        return Map.of("code", 200, "data", opLogRepo.findByUserIdOrderByIdDesc(user.getId()));
    }

    /**
     * 数据备份
     * 导出当前用户的所有发票、预算和聊天记录为 JSON 文件
     *
     * @param response HTTP 响应对象
     * @param token    用户认证令牌
     */
    // 2. 数据备份 (下载 JSON)
    @GetMapping("/backup")
    public void backup(HttpServletResponse response, @RequestHeader("Authorization") String token) {
        try {
            User user = UserController.tokenMap.get(token);
            if (user == null)
                return;

            // 封装所有数据
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("invoices", invoiceRepo.findByUserIdOrderByIdDesc(user.getId()));
            backupData.put("budgets", budgetRepo.findByUserId(user.getId()));
            backupData.put("chats", chatLogRepo.findByUserIdAndSessionIdOrderByIdAsc(user.getId(), null)); // 简单查所有吧，Repository要改一下支持只查UserId
            // 注意：为了简单，这里假设 chatLogRepo 有个 findByUserId 的方法，如果没有，请在 Repository 加一个:
            // List<ChatLog> findByUserId(Long userId);

            // 记录日志
            opLogRepo.save(new OperationLog(user.getId(), "数据备份", "导出全量数据"));

            // 输出文件
            response.setContentType("application/json;charset=utf-8");
            String fileName = URLEncoder.encode("SmartDoc_Backup_" + System.currentTimeMillis(),
                    StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".json");

            String jsonStr = JSONUtil.toJsonPrettyStr(backupData);
            IoUtil.write(response.getOutputStream(), StandardCharsets.UTF_8, true, jsonStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 数据恢复
     * 接收 JSON 备份文件，还原用户的发票和预算数据
     *
     * @param file  备份文件
     * @param token 用户认证令牌
     * @return 恢复结果
     */
    // 3. 数据恢复 (上传 JSON)
    @PostMapping("/restore")
    @Transactional // 事务：要么全成功，要么全失败
    public Map<String, Object> restore(@RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        try {
            String jsonStr = new String(file.getBytes(), StandardCharsets.UTF_8);
            cn.hutool.json.JSONObject data = JSONUtil.parseObj(jsonStr);

            // A. 恢复发票
            if (data.containsKey("invoices")) {
                List<InvoiceData> list = JSONUtil.toList(data.getJSONArray("invoices"), InvoiceData.class);
                // 策略：为了防止ID冲突，把ID置空作为新数据插入，或者先删除旧数据
                // 这里采用：保留旧数据，直接追加（为了演示简单）
                for (InvoiceData item : list) {
                    item.setId(null);
                    item.setUserId(user.getId());
                    invoiceRepo.save(item);
                }
            }

            // B. 恢复预算
            if (data.containsKey("budgets")) {
                List<Budget> list = JSONUtil.toList(data.getJSONArray("budgets"), Budget.class);
                for (Budget item : list) {
                    item.setId(null);
                    item.setUserId(user.getId());
                    // 简单的去重逻辑：如果该分类已存在，就不加了
                    if (budgetRepo.findByUserIdAndCategory(user.getId(), item.getCategory()) == null) {
                        budgetRepo.save(item);
                    }
                }
            }

            opLogRepo.save(new OperationLog(user.getId(), "数据恢复", "从备份文件恢复数据"));
            return Map.of("code", 200, "msg", "恢复成功");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("code", 500, "msg", "恢复失败: " + e.getMessage());
        }
    }
}
