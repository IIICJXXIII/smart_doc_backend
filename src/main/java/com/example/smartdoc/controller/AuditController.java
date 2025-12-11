package com.example.smartdoc.controller;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.OperationLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.repository.OperationLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审核管理控制器
 * 处理发票审核、驳回、查询待审核列表等管理端功能
 */
@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private OperationLogRepository logRepo;

    /**
     * 用户提交发票审核
     * 将发票状态从 草稿/驳回 (0/3) 修改为 待审核 (1)
     *
     * @param token 用户认证令牌
     * @param id    发票记录ID
     * @return 操作结果
     */
    // 1. [用户] 提交审核 (状态 0/3 -> 1)
    @PostMapping("/submit/{id}")
    public Map<String, Object> submit(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        User user = UserController.tokenMap.get(token);
        InvoiceData data = invoiceRepository.findById(id).orElse(null);

        if (data != null && data.getUserId().equals(user.getId())) {
            data.setStatus(1); // 变更为待审核
            data.setAuditRemark(null); // 清空旧的驳回原因
            invoiceRepository.save(data);
            return Map.of("code", 200, "msg", "已提交申请");
        }
        return Map.of("code", 403, "msg", "操作失败");
    }

    /**
     * [管理员] 获取所有待审核的发票列表
     * 仅限管理员角色访问
     *
     * @param token 用户认证令牌
     * @return 待审核发票列表
     */
    // 2. [管理员] 获取所有待审核列表 (跨用户)
    @GetMapping("/pending-list")
    public Map<String, Object> getPendingList(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null || !"admin".equals(user.getRole())) {
            return Map.of("code", 403, "msg", "无权访问");
        }

        // 查询所有 status = 1 的记录 (JPA方法需在Repository定义，或者用简单筛选)
        // 这里为了简单，我们用 Repository 的 findAll 过滤，实际建议写 SQL
        List<InvoiceData> all = invoiceRepository.findAll();
        List<InvoiceData> pending = all.stream().filter(i -> i.getStatus() == 1).toList();

        return Map.of("code", 200, "data", pending);
    }

    /**
     * [管理员] 审核通过
     * 将发票状态修改为 已通过 (2)，并记录操作日志
     *
     * @param token 用户认证令牌
     * @param id    发票记录ID
     * @return 操作结果
     */
    // 3. [管理员] 审核通过 (状态 1 -> 2)
    @PostMapping("/pass/{id}")
    @Transactional
    public Map<String, Object> pass(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        User user = UserController.tokenMap.get(token);
        if (user == null || !"admin".equals(user.getRole()))
            return Map.of("code", 403);

        InvoiceData data = invoiceRepository.findById(id).orElse(null);
        if (data != null) {
            data.setStatus(2);
            invoiceRepository.save(data);
            logRepo.save(new OperationLog(user.getId(), "审核通过", "单号:" + id));
            return Map.of("code", 200, "msg", "已批准");
        }
        return Map.of("code", 404);
    }

    /**
     * [管理员] 审核驳回
     * 将发票状态修改为 已驳回 (3)，记录驳回原因和操作日志
     *
     * @param token 用户认证令牌
     * @param id    发票记录ID
     * @param body  包含驳回原因的请求体 ("reason": "xxx")
     * @return 操作结果
     */
    // 4. [管理员] 驳回 (状态 1 -> 3)
    @PostMapping("/reject/{id}")
    @Transactional
    public Map<String, Object> reject(@RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = UserController.tokenMap.get(token);
        if (user == null || !"admin".equals(user.getRole()))
            return Map.of("code", 403);

        InvoiceData data = invoiceRepository.findById(id).orElse(null);
        if (data != null) {
            data.setStatus(3);
            data.setAuditRemark(body.get("reason"));
            invoiceRepository.save(data);
            logRepo.save(new OperationLog(user.getId(), "审核驳回", "单号:" + id + " 原因:" + body.get("reason")));
            return Map.of("code", 200, "msg", "已驳回");
        }
        return Map.of("code", 404);
    }
}
