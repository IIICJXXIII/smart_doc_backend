package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预算实体类
 * 对应数据库表 sys_budget
 * 用于存储用户对各类别的消费预算限额
 */
@Data
@Entity
@Table(name = "sys_budget")
public class Budget {
    /** 预算ID (主键) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 预算分类 (如: 餐饮美食, 交通出行) */
    private String category;

    /** 预算限额 (元) */
    private Double limitAmount;

    /** 已使用金额 (计算字段，非数据库列) */
    // 这个字段不存数据库，只用于返回给前端展示进度
    @Transient
    private Double usedAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
