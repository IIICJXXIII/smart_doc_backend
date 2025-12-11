package com.example.smartdoc.repository;

import com.example.smartdoc.model.InvoiceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 发票数据访问接口 - 核心业务数据仓库
 * 
 * <p>提供发票数据的 CRUD 操作和统计查询能力。
 * 由于 InvoiceData 使用了软删除注解，所有查询自动过滤已删除记录。</p>
 * 
 * <h3>JPA 方法命名规范:</h3>
 * <pre>
 * findBy + 字段名 + OrderBy + 排序字段 + Desc/Asc
 * 例: findByUserIdOrderByIdDesc
 * 翻译: SELECT * FROM invoice_record WHERE user_id = ? ORDER BY id DESC
 * </pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.model.InvoiceData
 */
@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceData, Long> {

    /**
     * 查询用户的所有发票（按 ID 倒序）
     * 
     * @param userId 用户 ID
     * @return 发票列表（最新的在前）
     */
    List<InvoiceData> findByUserIdOrderByIdDesc(Long userId);
    
    /**
     * 查询用户某类别的所有发票
     * 
     * @param userId   用户 ID
     * @param category 消费类别
     * @return 该类别的发票列表
     */
    List<InvoiceData> findByUserIdAndCategoryOrderByIdDesc(Long userId, String category);

    /**
     * 统计用户某类别的消费总额
     * <p>用于预算使用量计算，COALESCE 确保无数据时返回 0 而非 null。</p>
     * 
     * @param userId   用户 ID
     * @param category 消费类别
     * @return 该类别的消费总额
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InvoiceData i WHERE i.userId = :userId AND i.category = :category")
    Double sumAmountByUserIdAndCategory(Long userId, String category);

    /**
     * 获取用户近 12 个月的月度消费统计
     * <p>使用原生 SQL 按月份分组汇总金额，用于趋势预测分析。
     * 返回格式: [[月份, 总额], ...] 如 [["2024-01", 1234.56], ...]</p>
     * 
     * @param userId 用户 ID
     * @return 月度统计数据（最新月份在前）
     */
    @Query(value = "SELECT DATE_FORMAT(date, '%Y-%m') as month, SUM(amount) " +
            "FROM invoice_record " +
            "WHERE user_id = :userId " +
            "GROUP BY month " +
            "ORDER BY month DESC " +
            "LIMIT 12", nativeQuery = true)
    List<Object[]> findMonthlyStatsByUserId(Long userId);
}

