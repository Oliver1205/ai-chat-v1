package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.service.ChatMemoryManager;
import com.example.ai_chat_v1.service.LlmChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final LlmChatService chatService;
    private final ChatMemoryManager chatMemoryManager;

    public ChatController(LlmChatService chatService,
                          ChatMemoryManager chatMemoryManager) {
        this.chatService = chatService;
        this.chatMemoryManager = chatMemoryManager;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String sessionId,
                           @RequestParam String message) {
        SseEmitter emitter = new SseEmitter(60000L);

        chatService.streamChat(
                sessionId,
                message,
                token -> {
                    try {
                        emitter.send(token);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::complete,
                emitter::completeWithError
        );

        return emitter;
    }

    @GetMapping("/clear")
    public String clearMemory(@RequestParam String sessionId) {
        chatMemoryManager.clear(sessionId);
        return "✅ 用户 [" + sessionId + "] 的记忆已成功清空！可以开始全新的对话了。";
    }
}