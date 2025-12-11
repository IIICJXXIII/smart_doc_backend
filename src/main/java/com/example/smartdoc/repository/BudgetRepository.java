package com.example.smartdoc.repository;

import com.example.smartdoc.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 预算数据访问接口
 * 负责 Budget 实体的数据库操作 (CRUD)
 */
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    /**
     * 查询指定用户的所有预算设置
     *
     * @param userId 用户ID
     * @return 预算列表
     */
    List<Budget> findByUserId(Long userId);

    /**
     * 查询指定用户下特定分类的预算
     * (用于判断是否已存在预算，以便更新或新增)
     *
     * @param userId   用户ID
     * @param category 消费分类
     * @return 预算对象 (如果不存在则返回 null)
     */
    Budget findByUserIdAndCategory(Long userId, String category);
}
