package com.example.smartdoc.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartdoc.model.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * 回收站控制器 - 管理已软删除的票据数据
 * 
 * <p>该控制器提供回收站功能，支持查看已删除的票据、恢复误删数据、
 * 彻底删除（物理删除）以及一键清空回收站等操作。</p>
 * 
 * <h3>软删除机制说明:</h3>
 * <pre>
 * 普通删除 (DocController.deleteDoc)
 *     ↓ @SQLDelete 注解自动转换
 * UPDATE invoice_record SET is_deleted = 1 WHERE id = ?
 *     ↓
 * 数据进入回收站 (is_deleted = 1)
 *     ↓
 * 可选操作:
 *   - 还原 → UPDATE is_deleted = 0 → 数据恢复正常
 *   - 彻底删除 → DELETE → 数据永久消失
 * </pre>
 * 
 * <h3>技术实现:</h3>
 * <p>由于 InvoiceData 实体上有 @Where(clause = "is_deleted = 0") 注解，
 * JPA 的普通查询会自动过滤掉已删除数据。因此本控制器使用 EntityManager
 * 执行原生 SQL 来绕过该过滤条件。</p>
 * 
 * <h3>API 接口:</h3>
 * <ul>
 *   <li>GET /api/recycle/list - 获取回收站列表</li>
 *   <li>POST /api/recycle/restore/{id} - 还原票据</li>
 *   <li>DELETE /api/recycle/destroy/{id} - 彻底删除</li>
 *   <li>DELETE /api/recycle/clear-all - 清空回收站</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.model.InvoiceData
 */
@RestController  // RESTful 控制器
@RequestMapping("/api/recycle")  // URL 前缀
@CrossOrigin(origins = "*")  // 允许跨域
public class RecycleBinController {

    /**
     * JPA EntityManager - 用于执行原生 SQL 查询
     * 
     * <p>使用 @PersistenceContext 注入，可以执行原生 SQL，
     * 绕过实体上的 @Where 注解限制。</p>
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 获取回收站列表
     * 
     * <p>查询当前用户所有已软删除（is_deleted = 1）的票据。
     * 使用原生 SQL 绕过 @Where 注解的过滤。</p>
     * 
     * @param token 用户登录凭证
     * @return 已删除票据列表
     *         - code=200: 查询成功
     *         - code=401: 未登录
     */
    @GetMapping("/list")
    public Map<String, Object> getDeletedList(@RequestHeader("Authorization") String token) {
        // 1. 身份验证
        User user = UserController.tokenMap.get(token);
        if (user == null) {
            return Map.of("code", 401);
        }

        // 2. 使用原生 SQL 查询已删除数据
        // 关键: 绕过 @Where(clause = "is_deleted = 0") 注解
        String sql = "SELECT * FROM invoice_record WHERE user_id = :uid AND is_deleted = 1 ORDER BY id DESC";
        
        // 3. 执行查询并映射到 InvoiceData 实体
        List result = entityManager.createNativeQuery(sql, com.example.smartdoc.model.InvoiceData.class)
                .setParameter("uid", user.getId())  // 绑定用户 ID 参数
                .getResultList();

        return Map.of("code", 200, "data", result);
    }

    /**
     * 还原票据
     * 
     * <p>将已删除的票据从回收站恢复到正常状态，
     * 即将 is_deleted 字段从 1 更新为 0。</p>
     * 
     * @param id 票据主键 ID
     * @return 操作结果
     */
    @PostMapping("/restore/{id}")
    @Transactional  // 开启事务，确保数据一致性
    public Map<String, Object> restore(@PathVariable Long id) {
        // 执行原生 UPDATE 语句
        String sql = "UPDATE invoice_record SET is_deleted = 0 WHERE id = :id";
        entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .executeUpdate();
        
        return Map.of("code", 200, "msg", "还原成功");
    }

    /**
     * 彻底删除票据 (物理删除)
     * 
     * <p>从数据库中永久删除指定票据，该操作不可恢复。
     * 建议在前端增加二次确认提示。</p>
     * 
     * <h4>警告:</h4>
     * <p>此操作会永久删除数据，无法通过任何方式恢复！</p>
     * 
     * @param id 票据主键 ID
     * @return 操作结果
     */
    @DeleteMapping("/destroy/{id}")
    @Transactional  // 开启事务
    public Map<String, Object> destroy(@PathVariable Long id) {
        // 执行原生 DELETE 语句 (真正的物理删除)
        String sql = "DELETE FROM invoice_record WHERE id = :id";
        entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .executeUpdate();
        
        return Map.of("code", 200, "msg", "已彻底粉碎");
    }

    /**
     * 一键清空回收站
     * 
     * <p>批量删除当前用户回收站中的所有票据，
     * 该操作会永久删除所有已软删除的数据。</p>
     * 
     * <h4>警告:</h4>
     * <p>此操作不可恢复，请谨慎使用！</p>
     * 
     * @param token 用户登录凭证
     * @return 操作结果
     */
    @DeleteMapping("/clear-all")
    @Transactional  // 开启事务
    public Map<String, Object> clearAll(@RequestHeader("Authorization") String token) {
        // 1. 获取当前用户
        User user = UserController.tokenMap.get(token);
        
        // 2. 批量删除该用户的所有已删除数据
        String sql = "DELETE FROM invoice_record WHERE user_id = :uid AND is_deleted = 1";
        entityManager.createNativeQuery(sql)
                .setParameter("uid", user.getId())
                .executeUpdate();
        
        return Map.of("code", 200, "msg", "回收站已清空");
    }
}
