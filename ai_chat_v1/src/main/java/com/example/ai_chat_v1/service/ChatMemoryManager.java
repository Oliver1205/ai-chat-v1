package com.example.ai_chat_v1.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatMemoryManager {

    private final DbChatMemoryStore dbChatMemoryStore;

    // 👇 构造器注入：把我们刚写的数据库记忆存储器拿过来
    public ChatMemoryManager(DbChatMemoryStore dbChatMemoryStore) {
        this.dbChatMemoryStore = dbChatMemoryStore;
    }

    public ChatMemory getOrCreate(String sessionId) {
        return MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(20) // 依然保留最近 20 条的窗口机制（防止大模型被撑爆）
                .chatMemoryStore(dbChatMemoryStore) // 👈 灵魂挂载点：彻底替换掉内存存储！
                .build();
    }

    // 👇 补回被遗忘的清空方法
    public void clear(String sessionId) {
        // 直接调用我们刚才写的数据库删除功能
        dbChatMemoryStore.deleteMessages(sessionId);
    }
}