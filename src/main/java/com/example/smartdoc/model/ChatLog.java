package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天记录实体类
 * 对应数据库表 sys_chat_log
 * 用于存储用户与 AI 客服的所有对话历史
 */
@Data
@Entity
@Table(name = "sys_chat_log")
public class ChatLog {
    /** 日志ID (主键) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 消息角色: "user"(用户) 或 "ai"(系统回复) */
    private String role; // "user" 或 "ai"

    /** 消息内容 (支持长文本) */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 会话ID (用于区分不同的对话窗口) */
    private String sessionId;

    /** 创建时间 */
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
