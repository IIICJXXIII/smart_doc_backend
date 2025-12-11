package com.example.smartdoc.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 操作日志实体类 - 记录用户关键操作
 * 
 * <p>用于审计追踪，记录用户在系统中的重要操作，
 * 如登录、发票上传、数据删除等行为。</p>
 * 
 * <h3>日志记录场景:</h3>
 * <ul>
 *   <li>用户登录/登出</li>
 *   <li>发票上传、删除、审核</li>
 *   <li>数据备份、恢复</li>
 *   <li>其他关键业务操作</li>
 * </ul>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.SystemController
 */
@Data
@Entity
@Table(name = "sys_operation_log")
public class OperationLog {
    
    /** 日志主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 操作用户 ID */
    private Long userId;
    
    /** 操作类型 (如: 登录、上传发票、删除记录) */
    private String operation;
    
    /** 操作详情 - 具体的操作描述 */
    private String detail;
    
    /** 客户端 IP 地址 */
    private String ipAddress;
    
    /** 操作时间 */
    private LocalDateTime createTime;

    /**
     * JPA 生命周期回调 - 保存前自动设置创建时间
     */
    @PrePersist
    public void prePersist() { 
        this.createTime = LocalDateTime.now(); 
    }

    /**
     * 默认构造函数 (JPA 要求)
     */
    public OperationLog() {}
    
    /**
     * 便捷构造函数 - 快速创建日志记录
     * 
     * @param userId    操作用户 ID
     * @param op        操作类型
     * @param detail    操作详情
     */
    public OperationLog(Long userId, String op, String detail) {
        this.userId = userId;
        this.operation = op;
        this.detail = detail;
    }
}
