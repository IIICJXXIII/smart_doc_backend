package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 发票数据实体类
 * 对应数据库表 invoice_record
 * 核心业务表，存储所有OCR识别后的发票信息
 * 支持逻辑删除 (@SQLDelete) 和 自动过滤已删除数据 (@Where)
 */
@Data
@Entity // 1. 标记这是一个数据库实体
@Table(name = "invoice_record") // 2. 指定数据库表名为 invoice_record
@SQLDelete(sql = "UPDATE invoice_record SET is_deleted = 1 WHERE id = ?")
@Where(clause = "is_deleted = 0")
public class InvoiceData {

    @Id // 3. 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 4. 自增 ID
    private Long id;

    /** 商户名称 (销售方) */
    private String merchantName; // 商户名称

    /** 项目名称 (货物或服务名) */
    private String itemName; // 项目名称
    /** 开票日期 */
    private String date; // 开票日期

    /** 金额 (元) */
    private Double amount; // 金额

    /** 发票号码/车次号/订单号 */
    private String invoiceCode; // 发票号码

    /** 分类 (如: 餐饮美食, 交通出行) */
    private String category; // 分类

    /** 所属用户ID */
    private Long userId; // 用户id

    /** 异常标记: 0=正常, 1=疑似异常 (由异常检测算法计算) */
    private Integer isAnomaly; // 新增：异常标记 (0=正常, 1=异常)

    /** 逻辑删除标记: 0=未删除, 1=已删除 */
    private Integer isDeleted = 0;

    /** 审核状态: 0=草稿, 1=待审核, 2=已通过, 3=已驳回 */
    private Integer status = 1;

    /** 审核备注/驳回原因 */
    private String auditRemark;

    /** 原始图片 Base64 (字段不存库) */
    @Transient
    private String rawImageUrl;

    /** 创建时间 (自动记录) */
    private LocalDateTime createTime;

    // 在保存前自动填充创建时间
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

}