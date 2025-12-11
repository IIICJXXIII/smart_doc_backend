package com.example.smartdoc.repository;

import com.example.smartdoc.model.InvoiceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceData, Long> {

    // JPA 命名规范：findBy + 字段名 + 排序规则
    // 翻译成 SQL 就是：select * from invoice_record where user_id = ? order by id desc
    List<InvoiceData> findByUserIdOrderByIdDesc(Long userId);
    List<InvoiceData> findByUserIdAndCategoryOrderByIdDesc(Long userId, String category);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InvoiceData i WHERE i.userId = :userId AND i.category = :category")
    Double sumAmountByUserIdAndCategory(Long userId, String category);

    // 修改后：ORDER BY month DESC LIMIT 12 (先取最新的12个月)
    @Query(value = "SELECT DATE_FORMAT(date, '%Y-%m') as month, SUM(amount) " +
            "FROM invoice_record " +
            "WHERE user_id = :userId " +
            "GROUP BY month " +
            "ORDER BY month DESC " +
            "LIMIT 12", nativeQuery = true)
    List<Object[]> findMonthlyStatsByUserId(Long userId);
}

