package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.entity.ChatSession;
import com.example.ai_chat_v1.repository.ChatMessageRepository;
import com.example.ai_chat_v1.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SessionTitleService sessionTitleService;

    public SessionService(ChatSessionRepository sessionRepository,
                          ChatMessageRepository messageRepository,
                          SessionTitleService sessionTitleService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.sessionTitleService = sessionTitleService;
    }

    public List<ChatSession> getSessions() {
        return sessionRepository.findAllByOrderByLastActiveTimeDescCreateTimeDesc();
    }

    public List<ChatMessage> getMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    public ChatSession createSession() {
        LocalDateTime now = LocalDateTime.now();

        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setTitle("新对话");
        session.setTitleCustomized(false);
        session.setCreateTime(now);
        session.setLastActiveTime(now);

        return sessionRepository.save(session);
    }

    public ChatSession renameSession(String sessionId, String title) {
        return sessionTitleService.renameSession(sessionId, title);
    }
}