package com.example.smartdoc.repository;

import com.example.smartdoc.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 预算数据访问接口 - 管理预算记录的 CRUD 操作
 * 
 * <p>继承 JpaRepository 获得基础的增删改查能力，
 * 同时定义自定义查询方法满足业务需求。</p>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.model.Budget
 */
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    /**
     * 查询用户的所有预算设置
     * 
     * @param userId 用户 ID
     * @return 该用户的预算列表
     */
    List<Budget> findByUserId(Long userId);
    
    /**
     * 查询用户某个类别的预算
     * <p>用于检查某类别是否已设置预算，避免重复创建。</p>
     * 
     * @param userId   用户 ID
     * @param category 消费类别
     * @return 匹配的预算记录，不存在则返回 null
     */
    Budget findByUserIdAndCategory(Long userId, String category);
}
