package com.example.ai_chat_v1.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @Column(length = 36)
    private String id; // 会话的唯一 ID（我们将使用字母数字组合的 UUID）

    @Column(nullable = false)
    private String title; // 会话在侧边栏显示的标题

    @Column(name = "create_time")
    private LocalDateTime createTime; // 会话创建的时间

    // JPA 必须要求有一个无参构造函数
    public ChatSession() {
    }

    // --- 下面是 Getter 和 Setter 方法 ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}