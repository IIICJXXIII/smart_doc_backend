package com.example.smartdoc.repository;

import com.example.smartdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问接口
 * 负责 User 实体的数据库操作及身份查询
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 根据用户名查询用户
     * (用于登录校验和注册查重)
     *
     * @param username 用户名
     * @return 用户对象 (若不存在返回 null)
     */
    // JPA 会自动根据方法名生成 SQL：select * from sys_user where username = ?
    User findByUsername(String username);
}