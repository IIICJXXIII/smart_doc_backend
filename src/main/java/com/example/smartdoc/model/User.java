package com.example.smartdoc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 用户实体类 - 系统用户信息
 * 
 * <p>存储用户账号信息，用于登录认证和权限控制。
 * 系统支持两种角色：管理员(admin)和普通用户(user)。</p>
 * 
 * <h3>安全说明:</h3>
 * <p>当前密码以明文存储，仅用于演示目的。
 * 生产环境应使用 BCrypt 等算法加密存储。</p>
 * 
 * @author SmartDoc Team
 * @see com.example.smartdoc.controller.UserController
 */
@Data
@Entity
@Table(name = "sys_user")
public class User {
    
    /** 用户主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 
     * 用户名 (登录账号)
     * <p>唯一约束，不允许重复注册。</p>
     */
    @Column(unique = true, nullable = false)
    private String username;

    /** 
     * 登录密码
     * <p>注意：当前为明文存储，生产环境应加密。</p>
     */
    @Column(nullable = false)
    private String password;

    /** 用户昵称 - 显示名称 */
    private String nickname;
    
    /**
     * 用户角色
     * <ul>
     *   <li>"admin" - 管理员，拥有审核权限</li>
     *   <li>"user" - 普通用户</li>
     * </ul>
     */
    private String role;
}