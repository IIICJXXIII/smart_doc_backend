package com.example.smartdoc.controller;

import com.example.smartdoc.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 回收站管理控制器
 * 使用原生 SQL 处理逻辑删除 (is_deleted = 1) 的数据
 * 提供列表查询、还原、彻底删除和清空功能
 */
@RestController
@RequestMapping("/api/recycle")
@CrossOrigin(origins = "*")
public class RecycleBinController {

    @PersistenceContext
    private EntityManager entityManager; // 使用 EntityManager 执行原生操作

    /**
     * 获取回收站列表
     * 查询所有被标记为删除 (is_deleted = 1) 的记录
     *
     * @param token 用户认证令牌
     * @return 已删除的发票列表
     */
    // 1. 获取回收站列表 (查 is_deleted = 1)
    @GetMapping("/list")
    public Map<String, Object> getDeletedList(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        if (user == null)
            return Map.of("code", 401);

        // 使用原生 SQL 绕过 @Where 注解
        String sql = "SELECT * FROM invoice_record WHERE user_id = :uid AND is_deleted = 1 ORDER BY id DESC";
        List result = entityManager.createNativeQuery(sql, com.example.smartdoc.model.InvoiceData.class)
                .setParameter("uid", user.getId())
                .getResultList();

        return Map.of("code", 200, "data", result);
    }

    /**
     * 还原发票
     * 将发票标记为未删除 (is_deleted = 0)
     *
     * @param id 发票ID
     * @return 操作结果
     */
    // 2. 还原发票 (Update is_deleted = 0)
    @PostMapping("/restore/{id}")
    @Transactional
    public Map<String, Object> restore(@PathVariable Long id) {
        String sql = "UPDATE invoice_record SET is_deleted = 0 WHERE id = :id";
        entityManager.createNativeQuery(sql).setParameter("id", id).executeUpdate();
        return Map.of("code", 200, "msg", "还原成功");
    }

    /**
     * 彻底删除发票 (物理删除)
     * 从数据库中完全移除，无法恢复
     *
     * @param id 发票ID
     * @return 操作结果
     */
    // 3. 彻底删除 (Physical Delete)
    @DeleteMapping("/destroy/{id}")
    @Transactional
    public Map<String, Object> destroy(@PathVariable Long id) {
        String sql = "DELETE FROM invoice_record WHERE id = :id";
        entityManager.createNativeQuery(sql).setParameter("id", id).executeUpdate();
        return Map.of("code", 200, "msg", "已彻底粉碎");
    }

    /**
     * 清空回收站
     * 彻底删除当前用户所有在回收站中的记录
     *
     * @param token 用户认证令牌
     * @return 操作结果
     */
    // 4. 一键清空回收站
    @DeleteMapping("/clear-all")
    @Transactional
    public Map<String, Object> clearAll(@RequestHeader("Authorization") String token) {
        User user = UserController.tokenMap.get(token);
        String sql = "DELETE FROM invoice_record WHERE user_id = :uid AND is_deleted = 1";
        entityManager.createNativeQuery(sql).setParameter("uid", user.getId()).executeUpdate();
        return Map.of("code", 200, "msg", "回收站已清空");
    }
}
