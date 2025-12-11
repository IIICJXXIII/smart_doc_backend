package com.example.smartdoc.repository;

import com.example.smartdoc.model.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * 聊天记录数据访问接口
 * 负责 ChatLog 实体的数据库操作
 */
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    /**
     * 获取指定会话的完整聊天记录
     * 按照 ID 正序排列 (旧消息在前，新消息在后)
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 聊天记录列表
     */
    // 1. 获取某个具体的会话记录 (按时间正序)
    List<ChatLog> findByUserIdAndSessionIdOrderByIdAsc(Long userId, String sessionId);

    /**
     * 获取用户的所有会话ID列表
     * 使用 DISTINCT 去重，并按 SessionId 倒序 (模拟最新的会话在上面)
     *
     * @param userId 用户ID
     * @return 会话ID列表
     */
    // 2. 获取用户的所有会话ID列表 (去重，且按ID倒序，即最新的在前面)
    // 注意：这里只查 ID，为了性能
    @Query("SELECT DISTINCT c.sessionId FROM ChatLog c WHERE c.userId = :userId ORDER BY c.sessionId DESC")
    List<String> findSessionIdsByUserId(Long userId);
}