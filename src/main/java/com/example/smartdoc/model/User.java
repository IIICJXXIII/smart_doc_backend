package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_user") // 表名通常叫 sys_user 防止和数据库关键字冲突
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // 实际开发建议加密存储，这里作业演示存明文

    private String nickname; // 昵称
    private String role;     // 角色：admin, user
}