package com.example.ai_chat_v1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "title_customized", nullable = false)
    private Boolean titleCustomized = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;

    public ChatSession() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getTitleCustomized() {
        return titleCustomized;
    }

    public void setTitleCustomized(Boolean titleCustomized) {
        this.titleCustomized = titleCustomized;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}