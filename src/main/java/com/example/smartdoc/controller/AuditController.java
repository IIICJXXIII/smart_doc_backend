package com.example.smartdoc.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.OperationLog;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.repository.OperationLogRepository;

import jakarta.transaction.Transactional;

/**
 * 审批控制器 - 处理票据审核相关的业务逻辑
 * 
 * <p>该控制器实现了企业级的票据审批工作流，支持用户提交审核申请、
 * 管理员审核通过或驳回等功能。采用状态机模式管理票据的审批生命周期。</p>
 * 
 * <h3>票据状态流转:</h3>
 * <pre>
 * 状态码说明:
 *   0 - 草稿/初始状态 (刚上传，未提交审核)
 *   1 - 待审核 (用户已提交，等待管理员处理)
 *   2 - 已通过 (管理员审核通过)
 *   3 - 已驳回 (管理员驳回，可重新提交)
 * 
 * 状态流转图:
 *   [0:草稿] --提交审核--> [1:待审核] --审核通过--> [2:已通过]
 *                             |
 *                             +--驳回--> [3:已驳回] --重新提交--> [1:待审核]
 * </pre>
 * 
 * <h3>权限控制:</h3>
 * <ul>
 *   <li>普通用户: 只能提交自己的票据进行审核</li>
 *   <li>管理员 (role=admin): 可以查看所有待审核票据，执行通过/驳回操作</li>
 * </ul>
 * 
 * <h3>API 接口:</h3>
 * <ul>
 *   <li>POST /api/audit/submit/{id} - 用户提交审核</li>
 *   <li>GET /api/audit/pending-list - 管理员获取待审核列表</li>
 *   <li>POST /api/audit/pass/{id} - 管理员审核通过</li>
 *   <li>POST /api/audit/reject/{id} - 管理员驳回</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see InvoiceData#getStatus()
 */
@RestController  // 标记为 RESTful 控制器，返回值自动序列化为 JSON
@RequestMapping("/api/audit")  // 配置请求路径前缀
@CrossOrigin(origins = "*")  // 允许所有来源的跨域请求
public class AuditController {

    /** 票据数据仓库 - 用于操作 invoice_record 表 */
    @Autowired 
    private InvoiceRepository invoiceRepository;
    
    /** 操作日志仓库 - 用于记录审计日志 */
    @Autowired 
    private OperationLogRepository logRepo;

    /**
     * 【用户】提交审核申请
     * 
     * <p>将票据状态从草稿(0)或已驳回(3)变更为待审核(1)状态，
     * 同时清空之前可能存在的驳回原因。</p>
     * 
     * <h4>权限要求:</h4>
     * <p>用户只能提交属于自己的票据</p>
     * 
     * @param token 用户登录凭证，从请求头 Authorization 获取
     * @param id    票据主键 ID (从 URL 路径获取)
     * @return 操作结果
     *         - code=200: 提交成功
     *         - code=403: 权限不足或票据不存在
     */
    @PostMapping("/submit/{id}")
    public Map<String, Object> submit(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        // 1. 根据 Token 获取当前登录用户
        User user = UserController.tokenMap.get(token);
        
        // 2. 根据 ID 查询票据数据
        InvoiceData data = invoiceRepository.findById(id).orElse(null);

        // 3. 验证权限: 票据存在 且 属于当前用户
        if (data != null && data.getUserId().equals(user.getId())) {
            // 4. 更新状态为待审核
            data.setStatus(1);
            // 5. 清空旧的驳回原因 (如果是重新提交)
            data.setAuditRemark(null);
            // 6. 保存更新
            invoiceRepository.save(data);
            return Map.of("code", 200, "msg", "已提交申请");
        }
        
        // 权限校验失败
        return Map.of("code", 403, "msg", "操作失败");
    }

    /**
     * 【管理员】获取所有待审核票据列表
     * 
     * <p>查询系统中所有状态为"待审核"(status=1)的票据，
     * 该接口会返回跨用户的数据，仅管理员可访问。</p>
     * 
     * <h4>权限要求:</h4>
     * <p>仅角色为 admin 的用户可以调用此接口</p>
     * 
     * @param token 用户登录凭证
     * @return 待审核票据列表
     *         - code=200: 查询成功，data 包含票据列表
     *         - code=403: 权限不足
     */
    @GetMapping("/pending-list")
    public Map<String, Object> getPendingList(@RequestHeader("Authorization") String token) {
        // 1. 获取当前用户
        User user = UserController.tokenMap.get(token);
        
        // 2. 权限校验: 必须是管理员角色
        if (user == null || !"admin".equals(user.getRole())) {
            return Map.of("code", 403, "msg", "无权访问");
        }

        // 3. 查询所有票据 (实际项目建议在 Repository 层写 findByStatus 方法)
        List<InvoiceData> all = invoiceRepository.findAll();
        
        // 4. 过滤出状态为 1 (待审核) 的记录
        List<InvoiceData> pending = all.stream()
                .filter(i -> i.getStatus() == 1)
                .toList();

        return Map.of("code", 200, "data", pending);
    }

    /**
     * 【管理员】审核通过
     * 
     * <p>将票据状态从待审核(1)变更为已通过(2)，
     * 同时记录操作审计日志。</p>
     * 
     * <h4>权限要求:</h4>
     * <p>仅角色为 admin 的用户可以调用此接口</p>
     * 
     * @param token 用户登录凭证
     * @param id    票据主键 ID
     * @return 操作结果
     *         - code=200: 审核通过成功
     *         - code=403: 权限不足
     *         - code=404: 票据不存在
     */
    @PostMapping("/pass/{id}")
    @Transactional  // 开启事务，确保状态更新和日志记录的原子性
    public Map<String, Object> pass(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        // 1. 权限校验
        User user = UserController.tokenMap.get(token);
        if (user == null || !"admin".equals(user.getRole())) {
            return Map.of("code", 403);
        }

        // 2. 查询并更新票据状态
        InvoiceData data = invoiceRepository.findById(id).orElse(null);
        if (data != null) {
            data.setStatus(2);  // 状态改为已通过
            invoiceRepository.save(data);
            
            // 3. 记录操作审计日志
            logRepo.save(new OperationLog(user.getId(), "审核通过", "单号:" + id));
            
            return Map.of("code", 200, "msg", "已批准");
        }
        
        return Map.of("code", 404);
    }

    /**
     * 【管理员】驳回申请
     * 
     * <p>将票据状态从待审核(1)变更为已驳回(3)，
     * 并记录驳回原因，用户可根据原因修改后重新提交。</p>
     * 
     * <h4>权限要求:</h4>
     * <p>仅角色为 admin 的用户可以调用此接口</p>
     * 
     * <h4>请求体格式:</h4>
     * <pre>
     * {
     *   "reason": "发票金额与实际不符，请核实后重新提交"
     * }
     * </pre>
     * 
     * @param token 用户登录凭证
     * @param id    票据主键 ID
     * @param body  请求体，包含 reason 字段表示驳回原因
     * @return 操作结果
     */
    @PostMapping("/reject/{id}")
    @Transactional  // 开启事务
    public Map<String, Object> reject(@RequestHeader("Authorization") String token,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        // 1. 权限校验
        User user = UserController.tokenMap.get(token);
        if (user == null || !"admin".equals(user.getRole())) {
            return Map.of("code", 403);
        }

        // 2. 查询并更新票据
        InvoiceData data = invoiceRepository.findById(id).orElse(null);
        if (data != null) {
            data.setStatus(3);  // 状态改为已驳回
            data.setAuditRemark(body.get("reason"));  // 保存驳回原因
            invoiceRepository.save(data);
            
            // 3. 记录操作审计日志 (包含驳回原因)
            logRepo.save(new OperationLog(user.getId(), "审核驳回", 
                    "单号:" + id + " 原因:" + body.get("reason")));
            
            return Map.of("code", 200, "msg", "已驳回");
        }
        
        return Map.of("code", 404);
    }
}
