package com.example.smartdoc.repository;

import com.example.smartdoc.model.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 操作日志数据访问接口 - 管理用户操作审计记录
 * 
 * <p>提供操作日志的存储和查询能力，用于系统审计和操作追踪。</p>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.model.OperationLog
 * @see com.example.smartdoc.controller.SystemController
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    /**
     * 查询用户的操作日志（按 ID 倒序）
     * <p>最新的操作记录排在前面，便于查看近期操作。</p>
     * 
     * @param userId 用户 ID
     * @return 操作日志列表
     */
    List<OperationLog> findByUserIdOrderByIdDesc(Long userId);
}
