package com.example.smartdoc.repository;

import com.example.smartdoc.model.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * 对话日志数据访问接口 - 管理 AI 对话历史记录
 * 
 * <p>提供对话记录的存储和查询能力，支持会话管理功能，
 * 让用户可以查看历史对话并继续之前的会话。</p>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.model.ChatLog
 * @see com.example.smartdoc.controller.ChatServer
 */
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    /**
     * 获取指定会话的所有对话记录
     * <p>按 ID 正序排列，确保消息按时间顺序展示。</p>
     * 
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 该会话的对话记录列表（按时间正序）
     */
    List<ChatLog> findByUserIdAndSessionIdOrderByIdAsc(Long userId, String sessionId);

    /**
     * 获取用户的所有会话 ID 列表
     * <p>去重查询，只返回不同的 sessionId，用于侧边栏展示会话列表。
     * 按会话 ID 倒序，最新的会话排在前面。</p>
     * 
     * @param userId 用户 ID
     * @return 去重后的会话 ID 列表
     */
    @Query("SELECT DISTINCT c.sessionId FROM ChatLog c WHERE c.userId = :userId ORDER BY c.sessionId DESC")
    List<String> findSessionIdsByUserId(Long userId);
}