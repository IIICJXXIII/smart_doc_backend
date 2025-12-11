package com.example.smartdoc.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

/**
 * 发票数据实体类 - 核心业务对象
 * 
 * <p>存储通过 OCR 识别后的发票信息，支持软删除和审批流程。
 * 这是系统最重要的数据模型，贯穿发票上传、识别、审核、统计全流程。</p>
 * 
 * <h3>软删除机制:</h3>
 * <p>使用 Hibernate 的 @SQLDelete 和 @Where 注解实现逻辑删除：
 * 删除时将 is_deleted 设为 1，查询时自动过滤已删除记录。</p>
 * 
 * <h3>审批状态 (status):</h3>
 * <ul>
 *   <li>0 - 待审核（新上传的发票）</li>
 *   <li>1 - 已通过（审核通过）</li>
 *   <li>2 - 已驳回（审核不通过）</li>
 * </ul>
 * 
 * <h3>异常标记 (isAnomaly):</h3>
 * <ul>
 *   <li>0 - 正常发票</li>
 *   <li>1 - 异常发票（通过 Z-Score 算法检测）</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.DocController
 * @see com.example.smartdoc.utils.AnomalyDetectionUtil
 */
@Data
@Entity
@Table(name = "invoice_record")
@SQLDelete(sql = "UPDATE invoice_record SET is_deleted = 1 WHERE id = ?")
@Where(clause = "is_deleted = 0")
public class InvoiceData {

    /** 发票记录主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商户名称 - OCR 识别的销售方名称 */
    private String merchantName;
    
    /** 项目名称 - 发票上的商品或服务名称 */
    private String itemName;
    
    /** 开票日期 - 格式如 "2024-01-15" */
    private String date;
    
    /** 金额 - 发票总金额（含税） */
    private Double amount;
    
    /** 发票号码 - 发票的唯一编号 */
    private String invoiceCode;
    
    /** 分类 - 消费类别（餐饮、交通、办公用品等） */
    private String category;
    
    /** 用户 ID - 发票所属用户 */
    private Long userId;
    
    /** 异常标记: 0=正常, 1=异常（金额异常高或低） */
    private Integer isAnomaly;
    
    /** 删除标记: 0=正常, 1=已删除（软删除） */
    private Integer isDeleted = 0;
    
    /** 审批状态: 0=待审核, 1=已通过, 2=已驳回 */
    private Integer status = 1;
    
    /** 审批备注 - 审核人填写的说明 */
    private String auditRemark;

    /**
     * 原始图片 URL (不持久化)
     * <p>发票原图的临时访问路径，用于前端预览。</p>
     */
    @Transient
    private String rawImageUrl;
    
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