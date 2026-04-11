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
    private final ChatMemoryManager chatMemoryManager; // 引入记忆管理员以便调用 clear 方法

    // 构造器注入
    public ChatController(LlmChatService chatService, ChatMemoryManager chatMemoryManager) {
        this.chatService = chatService;
        this.chatMemoryManager = chatMemoryManager;
    }

    /**
     * 核心接口 1：流式对话（带记忆）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestParam(defaultValue = "default-user") String sessionId,
            @RequestParam(defaultValue = "你好") String message) {

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
                () -> emitter.complete(),
                error -> emitter.completeWithError(error)
        );

        return emitter;
    }

    /**
     * 核心接口 2：清空记忆（相当于开启新会话）
     */
    @GetMapping("/clear")
    public String clearMemory(@RequestParam(defaultValue = "default-user") String sessionId) {
        chatMemoryManager.clear(sessionId);
        return "✅ 用户 [" + sessionId + "] 的记忆已成功清空！可以开始全新的对话了。";
    }
}