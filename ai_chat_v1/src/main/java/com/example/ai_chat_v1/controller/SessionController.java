package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.dto.RenameSessionRequest;
import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.entity.ChatSession;
import com.example.ai_chat_v1.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/sessions")
    public List<ChatSession> getSessions() {
        return sessionService.getSessions();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessage> getMessages(@PathVariable String sessionId) {
        return sessionService.getMessages(sessionId);
    }

    @PostMapping("/sessions")
    public ChatSession createSession() {
        return sessionService.createSession();
    }

    @PutMapping("/sessions/{sessionId}/title")
    public ChatSession renameSession(@PathVariable String sessionId,
                                     @Valid @RequestBody RenameSessionRequest request) {
        return sessionService.renameSession(sessionId, request.title());
    }
}