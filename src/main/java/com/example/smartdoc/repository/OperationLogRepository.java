package com.example.smartdoc.repository;

import com.example.smartdoc.model.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 操作日志数据访问接口
 * 负责 OperationLog 实体的数据库操作
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    /**
     * 获取指定用户的操作日志
     * 按 ID 倒序排列
     *
     * @param userId 用户ID
     * @return 日志列表
     */
    List<OperationLog> findByUserIdOrderByIdDesc(Long userId);
}
