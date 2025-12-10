package com.example.smartdoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_chat_log")
public class ChatLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String role; // "user" 或 "ai"


    @Column(columnDefinition = "TEXT")
    private String content;

    private String sessionId;
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
