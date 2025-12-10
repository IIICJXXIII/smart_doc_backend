package com.example.smartdoc.repository;

import com.example.smartdoc.model.ChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    // 1. 获取某个具体的会话记录 (按时间正序)
    List<ChatLog> findByUserIdAndSessionIdOrderByIdAsc(Long userId, String sessionId);

    // 2. 获取用户的所有会话ID列表 (去重，且按ID倒序，即最新的在前面)
    // 注意：这里只查 ID，为了性能
    @Query("SELECT DISTINCT c.sessionId FROM ChatLog c WHERE c.userId = :userId ORDER BY c.sessionId DESC")
    List<String> findSessionIdsByUserId(Long userId);
}