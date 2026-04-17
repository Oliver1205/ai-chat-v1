package com.example.ai_chat_v1.service;

import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionContextService {

    private final SessionTitleService sessionTitleService;
    private final ChatMemoryManager memoryManager;

    public ChatSessionContextService(SessionTitleService sessionTitleService,
                                     ChatMemoryManager memoryManager) {
        this.sessionTitleService = sessionTitleService;
        this.memoryManager = memoryManager;
    }

    public ChatMemory prepare(String sessionId) {
        sessionTitleService.ensureSessionExists(sessionId);
        sessionTitleService.touchSession(sessionId);
        return memoryManager.getOrCreate(sessionId);
    }
}