package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 对应数据库表 sys_operation_log
 * 记录关键操作（如审核、备份、恢复），用于审计和追踪
 */
@Data
@Entity
@Table(name = "sys_operation_log")
public class OperationLog {
    /** 日志ID (主键) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** 操作用户ID */
    private Long userId;

    /** 操作类型 (如: 审核通过, 数据备份) */
    private String operation;

    /** 操作详情/备注 */
    private String detail;

    /** 操作来源IP (预留字段) */
    private String ipAddress;

    /** 操作时间 */
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

    // 构造函数方便调用
    public OperationLog() {
    }

    public OperationLog(Long userId, String op, String detail) {
        this.userId = userId;
        this.operation = op;
        this.detail = detail;
    }
}
