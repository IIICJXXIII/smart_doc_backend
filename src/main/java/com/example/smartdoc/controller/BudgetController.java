package com.example.smartdoc.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartdoc.model.Budget;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.BudgetRepository;
import com.example.smartdoc.repository.InvoiceRepository;

/**
 * 预算管理控制器 - 处理用户消费预算的增删改查
 * 
 * <p>该控制器提供分类预算管理功能，允许用户为不同消费类别（如餐饮、交通等）
 * 设置预算上限，并实时计算各分类的已使用金额和剩余额度。</p>
 * 
 * <h3>核心功能:</h3>
 * <ul>
 *   <li>预算列表查询: 获取用户所有预算及使用进度</li>
 *   <li>预算设置/更新: 新增或修改分类预算额度</li>
 *   <li>预算删除: 移除不需要的预算配置</li>
 * </ul>
 * 
 * <h3>预算进度计算逻辑:</h3>
 * <pre>
 * 已使用金额 = SUM(该用户该分类下所有发票的 amount)
 * 使用进度% = 已使用金额 / 预算上限 × 100%
 * </pre>
 * 
 * <h3>API 接口:</h3>
 * <ul>
 *   <li>GET /api/budget/list - 获取预算列表 (含进度)</li>
 *   <li>POST /api/budget/save - 新增/更新预算</li>
 *   <li>DELETE /api/budget/delete/{id} - 删除预算</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see Budget
 */
@RestController  // 标记为 RESTful 控制器
@RequestMapping("/api/budget")  // 配置请求路径前缀
@CrossOrigin(origins = "*")  // 允许跨域请求
public class BudgetController {

    /** 预算数据仓库 - 用于操作 sys_budget 表 */
    @Autowired
    private BudgetRepository budgetRepository;

    /** 票据数据仓库 - 用于统计各分类消费金额 */
    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * 获取预算列表 (带使用进度计算)
     * 
     * <p>该接口返回当前用户的所有预算配置，并实时计算每个分类的已使用金额。
     * 前端可根据返回数据展示预算进度条、超支预警等信息。</p>
     * 
     * <h4>返回数据示例:</h4>
     * <pre>
     * {
     *   "code": 200,
     *   "data": [
     *     {
     *       "id": 1,
     *       "category": "餐饮美食",
     *       "limitAmount": 3000.00,    // 预算上限
     *       "usedAmount": 2350.50      // 已使用金额 (实时计算)
     *     },
     *     ...
     *   ]
     * }
     * </pre>
     * 
     * @param token 用户登录凭证
     * @return 预算列表及使用进度
     */
    @GetMapping("/list")
    public Map<String, Object> getList(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);  // 未授权
        }

        // 2. 查询该用户的所有预算配置
        List<Budget> budgets = budgetRepository.findByUserId(user.getId());

        // 3. 核心逻辑: 遍历每个预算，实时计算已使用金额
        for (Budget b : budgets) {
            // 调用 InvoiceRepository 的聚合查询方法
            // SQL: SELECT SUM(amount) FROM invoice_record WHERE user_id=? AND category=?
            Double used = invoiceRepository.sumAmountByUserIdAndCategory(user.getId(), b.getCategory());
            
            // 将计算结果设置到 @Transient 字段 (不会存入数据库)
            b.setUsedAmount(used);
        }

        return Map.of("code", 200, "data", budgets);
    }

    /**
     * 设置或更新预算
     * 
     * <p>该接口采用"存在则更新，不存在则新增"的策略 (Upsert 模式)。
     * 同一用户的同一分类只能有一个预算配置。</p>
     * 
     * <h4>请求体示例:</h4>
     * <pre>
     * {
     *   "category": "餐饮美食",
     *   "limitAmount": 3000.00
     * }
     * </pre>
     * 
     * <h4>业务逻辑:</h4>
     * <ol>
     *   <li>查询该用户是否已有该分类的预算</li>
     *   <li>如果存在: 更新预算金额</li>
     *   <li>如果不存在: 创建新的预算记录</li>
     * </ol>
     * 
     * @param token  用户登录凭证
     * @param budget 预算数据，包含 category 和 limitAmount
     * @return 操作结果
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestHeader("Authorization") String token, @RequestBody Budget budget) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        // 2. 查询是否已存在该分类的预算
        Budget exist = budgetRepository.findByUserIdAndCategory(user.getId(), budget.getCategory());
        
        if (exist != null) {
            // 3a. 已存在: 更新预算金额
            exist.setLimitAmount(budget.getLimitAmount());
            budgetRepository.save(exist);
        } else {
            // 3b. 不存在: 绑定用户 ID 后新增
            budget.setUserId(user.getId());
            budgetRepository.save(budget);
        }
        
        return Map.of("code", 200, "msg", "设置成功");
    }

    /**
     * 删除预算
     * 
     * <p>根据预算 ID 删除指定的预算配置。
     * 删除后该分类将不再有预算限制。</p>
     * 
     * <h4>注意事项:</h4>
     * <p>当前实现未做用户归属校验，建议生产环境补充权限检查</p>
     * 
     * @param token 用户登录凭证
     * @param id    预算主键 ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        // 直接删除 (建议补充: 验证该预算是否属于当前用户)
        budgetRepository.deleteById(id);
        return Map.of("code", 200, "msg", "已删除");
    }
}