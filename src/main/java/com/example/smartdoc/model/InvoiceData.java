package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity // 1. 标记这是一个数据库实体
@Table(name = "invoice_record") // 2. 指定数据库表名为 invoice_record
public class InvoiceData {

    @Id // 3. 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 4. 自增 ID
    private Long id;

    private String merchantName; // 商户名称
    private String itemName;     // 项目名称
    private String date;         // 开票日期
    private Double amount;       // 金额
    private String invoiceCode;  // 发票号码
    private String category;     // 分类
    private Long userId;         //用户id

    @Transient
    private String rawImageUrl;
    // 创建时间 (自动记录)
    private LocalDateTime createTime;

    // 在保存前自动填充创建时间
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

}