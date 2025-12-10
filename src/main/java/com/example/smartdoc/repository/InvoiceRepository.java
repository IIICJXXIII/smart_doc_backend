package com.example.smartdoc.repository;

import com.example.smartdoc.model.InvoiceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceData, Long> {

    // JPA 命名规范：findBy + 字段名 + 排序规则
    // 翻译成 SQL 就是：select * from invoice_record where user_id = ? order by id desc
    List<InvoiceData> findByUserIdOrderByIdDesc(Long userId);
}