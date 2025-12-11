package com.example.smartdoc.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 对话日志实体类 - 记录用户与 AI 的对话历史
 * 
 * <p>每条记录代表一轮对话中的一条消息（用户提问或 AI 回复）。
 * 同一会话的消息通过 sessionId 关联，支持多会话管理。</p>
 * 
 * <h3>数据结构:</h3>
 * <pre>
 * 会话1 (sessionId=xxx)
 *   ├─ 消息1: role=user, content="查询本月餐饮支出"
 *   └─ 消息2: role=ai, content="本月餐饮支出共计 1234.56 元..."
 * 会话2 (sessionId=yyy)
 *   ├─ 消息1: role=user, content="..."
 *   └─ ...
 * </pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.ChatServer
 */
@Data
@Entity
@Table(name = "sys_chat_log")
public class ChatLog {
    
    /** 消息主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID - 消息所属用户 */
    private Long userId;
    
    /**
     * 消息角色
     * <ul>
     *   <li>"user" - 用户发送的消息</li>
     *   <li>"ai" - AI 回复的消息</li>
     * </ul>
     */
    private String role;

    /** 消息内容 (TEXT 类型，支持长文本) */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 会话 ID - 同一会话的消息共享相同的 sessionId */
    private String sessionId;
    
    /** 消息创建时间 */
    private LocalDateTime createTime;

    /**
     * JPA 生命周期回调 - 保存前自动设置创建时间
     */
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
