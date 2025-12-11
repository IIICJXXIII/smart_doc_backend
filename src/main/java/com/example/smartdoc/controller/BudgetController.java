package com.example.smartdoc.controller;

import com.example.smartdoc.model.Budget;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.BudgetRepository;
import com.example.smartdoc.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预算管理控制器
 * 处理用户预算的设置、查询、删除以及计算已用金额
 */
@RestController
@RequestMapping("/api/budget")
@CrossOrigin(origins = "*")
public class BudgetController {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * 获取当前用户的预算列表
     * 同时计算每个预算分类下的已使用金额
     *
     * @param token 用户认证令牌
     * @return 预算列表（包含已用金额）
     */
    // 1. 获取预算列表 (带进度计算)
    @GetMapping("/list")
    public Map<String, Object> getList(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        List<Budget> budgets = budgetRepository.findByUserId(user.getId());

        // 核心逻辑：遍历每个预算，去发票表查查花了多少钱
        for (Budget b : budgets) {
            Double used = invoiceRepository.sumAmountByUserIdAndCategory(user.getId(), b.getCategory());
            b.setUsedAmount(used);
        }

        return Map.of("code", 200, "data", budgets);
    }

    /**
     * 设置或更新预算
     * 如果该分类已存在预算则更新限额，否则新增预算记录
     *
     * @param token  用户认证令牌
     * @param budget 预算对象
     * @return 操作结果
     */
    // 2. 设置/更新预算
    @PostMapping("/save")
    public Map<String, Object> save(@RequestHeader("Authorization") String token, @RequestBody Budget budget) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        // 检查该分类是否已存在预算，存在则更新，不存在则新增
        Budget exist = budgetRepository.findByUserIdAndCategory(user.getId(), budget.getCategory());
        if (exist != null) {
            exist.setLimitAmount(budget.getLimitAmount());
            budgetRepository.save(exist);
        } else {
            budget.setUserId(user.getId());
            budgetRepository.save(budget);
        }
        return Map.of("code", 200, "msg", "设置成功");
    }

    /**
     * 删除指定的预算规则
     *
     * @param token 用户认证令牌
     * @param id    预算ID
     * @return 操作结果
     */
    // 3. 删除预算
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        budgetRepository.deleteById(id);
        return Map.of("code", 200, "msg", "已删除");
    }
}