package com.example.smartdoc.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

/**
 * 预算实体类 - 管理各类别的消费预算
 * 
 * <p>用户可以为不同消费类别（如餐饮、交通、办公用品等）设置预算上限，
 * 系统会在发票录入时自动计算已使用额度并提供预警。</p>
 * 
 * <h3>业务场景:</h3>
 * <ul>
 *   <li>用户设置类别预算上限</li>
 *   <li>查看预算使用进度（usedAmount 由查询时计算填充）</li>
 *   <li>发票保存时检查是否超预算</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.BudgetController
 */
@Data
@Entity
@Table(name = "sys_budget")
public class Budget {
    
    /** 预算记录主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户 ID */
    private Long userId;
    
    /** 消费类别名称 (如: 餐饮、交通、办公用品) */
    private String category;
    
    /** 预算上限金额 */
    private Double limitAmount;

    /**
     * 已使用金额 (不持久化)
     * <p>此字段不存入数据库，仅在查询时动态计算并填充，
     * 用于前端展示预算使用进度条。</p>
     */
    @Transient
    private Double usedAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * JPA 生命周期回调 - 保存前自动设置创建时间
     */
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
