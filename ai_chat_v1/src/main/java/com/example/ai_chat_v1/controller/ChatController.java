package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.agent.studycoach.StudyCoachAgentService;
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
    private final StudyCoachAgentService studyCoachAgentService;
    private final ChatMemoryManager chatMemoryManager;

    public ChatController(LlmChatService chatService,
                          StudyCoachAgentService studyCoachAgentService,
                          ChatMemoryManager chatMemoryManager) {
        this.chatService = chatService;
        this.studyCoachAgentService = studyCoachAgentService;
        this.chatMemoryManager = chatMemoryManager;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String sessionId,
                           @RequestParam String message,
                           @RequestParam(defaultValue = "default") String mode) {
        SseEmitter emitter = new SseEmitter(60000L);

        if ("study-coach".equalsIgnoreCase(mode)) {
            studyCoachAgentService.streamChat(
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
