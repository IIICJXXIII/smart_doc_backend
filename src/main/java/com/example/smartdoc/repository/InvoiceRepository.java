package com.example.smartdoc.repository;

import com.example.smartdoc.model.InvoiceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 发票数据访问接口
 * 负责 InvoiceData 实体的 CRUD 及其统计查询
 */
@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceData, Long> {

    /**
     * 查询指定用户的所有发票记录
     * 按 ID 倒序排列 (最新的在前)
     *
     * @param userId 用户ID
     * @return 发票列表
     */
    // JPA 命名规范：findBy + 字段名 + 排序规则
    // 翻译成 SQL 就是：select * from invoice_record where user_id = ? order by id desc
    List<InvoiceData> findByUserIdOrderByIdDesc(Long userId);

    /**
     * 查询用户在指定分类下的所有发票
     * (用于异常检测算法的历史数据对比)
     *
     * @param userId   用户ID
     * @param category 分类
     * @return 发票列表
     */
    List<InvoiceData> findByUserIdAndCategoryOrderByIdDesc(Long userId, String category);

    /**
     * 统计用户在指定分类下的总支出金额
     * (用于预算进度计算)
     *
     * @param userId   用户ID
     * @param category 分类
     * @return 总金额 (如果无数据则返回 0)
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InvoiceData i WHERE i.userId = :userId AND i.category = :category")
    Double sumAmountByUserIdAndCategory(Long userId, String category);

    /**
     * 统计用户每月的总支出
     * 用于前端趋势图展示 (Trend Chart)
     * 只取最近 12 个月的数据
     *
     * @param userId 用户ID
     * @return Object数组列表 ([month, total_amount])
     */
    // 修改后：ORDER BY month DESC LIMIT 12 (先取最新的12个月)
    @Query(value = "SELECT DATE_FORMAT(date, '%Y-%m') as month, SUM(amount) " +
            "FROM invoice_record " +
            "WHERE user_id = :userId " +
            "GROUP BY month " +
            "ORDER BY month DESC " +
            "LIMIT 12", nativeQuery = true)
    List<Object[]> findMonthlyStatsByUserId(Long userId);
}
