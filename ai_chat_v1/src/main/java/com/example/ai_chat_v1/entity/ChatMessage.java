package com.example.ai_chat_v1.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 每条消息的自增主键

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId; // 这条消息属于哪个会话？

    @Column(nullable = false, length = 20)
    private String role; // 说话人的角色：USER 或者 AI

    // 🚨 极其关键：大模型的回答可能非常长，必须强制指定为 TEXT 类型！
    // 否则默认的 VARCHAR(255) 存几个字就爆了！
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 具体的聊天内容

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public ChatMessage() {
    }

    // --- 下面是 Getter 和 Setter 方法 ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}