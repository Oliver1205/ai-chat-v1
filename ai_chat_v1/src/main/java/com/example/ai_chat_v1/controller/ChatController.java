package com.example.ai_chat_v1.controller;

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

    public ChatController(LlmChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam(defaultValue = "你好，请自我介绍") String message) {

        SseEmitter emitter = new SseEmitter(60000L);

        chatService.streamChat(
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
}