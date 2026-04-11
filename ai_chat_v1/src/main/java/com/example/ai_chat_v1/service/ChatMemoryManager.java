package com.example.ai_chat_v1.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatMemoryManager {

    private final Map<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();

    // 1. 获取或创建记忆
    public ChatMemory getOrCreate(String sessionId) {
        return memoryMap.computeIfAbsent(sessionId, id -> {
            ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
            memory.add(SystemMessage.from("你是一个耐心、清晰、友好的 AI 助手。回答时尽量条理化，优先使用中文。"));
            return memory;
        });
    }

    // 2. 清空指定用户的记忆（解决你的报错！）
    public void clear(String sessionId) {
        memoryMap.remove(sessionId);
    }
}