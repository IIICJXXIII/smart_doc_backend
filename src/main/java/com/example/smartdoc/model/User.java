package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 用户实体类
 * 对应数据库表 sys_user
 * 简单的用户权限模型，区分普通用户和管理员
 */
@Data
@Entity
@Table(name = "sys_user") // 表名通常叫 sys_user 防止和数据库关键字冲突
public class User {
    /** 用户ID (主键) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名 (唯一) */
    @Column(unique = true, nullable = false)
    private String username;

    /** 密码 (实际生产应加密，此处为演示存明文) */
    @Column(nullable = false)
    private String password; // 实际开发建议加密存储，这里作业演示存明文

    /** 用户昵称 */
    private String nickname; // 昵称

    /** 角色：user(普通用户), admin(管理员) */
    private String role; // 角色：admin, user
}