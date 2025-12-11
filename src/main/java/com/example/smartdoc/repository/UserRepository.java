package com.example.smartdoc.repository;

import com.example.smartdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问接口 - 管理用户账号信息
 * 
 * <p>提供用户数据的 CRUD 操作，支持按用户名查询等自定义方法。</p>
 * 
 * <h3>JPA 方法名解析:</h3>
 * <pre>
 * findByUsername(String username)
 * → SELECT * FROM sys_user WHERE username = ?
 * </pre>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.model.User
 * @see com.example.smartdoc.controller.UserController
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户
     * <p>用于登录验证和注册时检查用户名是否已存在。</p>
     * 
     * @param username 用户名
     * @return 匹配的用户对象，不存在则返回 null
     */
    User findByUsername(String username);
}