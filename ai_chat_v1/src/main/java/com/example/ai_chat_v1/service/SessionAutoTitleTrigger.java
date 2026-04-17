package com.example.ai_chat_v1.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class SessionAutoTitleTrigger {

    private final SessionTitleService sessionTitleService;

    public SessionAutoTitleTrigger(SessionTitleService sessionTitleService) {
        this.sessionTitleService = sessionTitleService;
    }

    public void triggerAsync(String sessionId) {
        CompletableFuture.runAsync(() -> {
            try {
                sessionTitleService.tryAutoGenerateTitle(sessionId);
            } catch (Exception ignored) {
            }
        });
    }
}