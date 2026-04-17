package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.dto.RenameSessionRequest;
import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.entity.ChatSession;
import com.example.ai_chat_v1.repository.ChatMessageRepository;
import com.example.ai_chat_v1.repository.ChatSessionRepository;
import com.example.ai_chat_v1.service.ChatMemoryManager;
import com.example.ai_chat_v1.service.KnowledgeBaseManager;
import com.example.ai_chat_v1.service.LlmChatService;
import com.example.ai_chat_v1.service.SessionTitleService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final LlmChatService chatService;
    private final ChatMemoryManager chatMemoryManager;
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SessionTitleService sessionTitleService;

    public ChatController(LlmChatService chatService,
                          ChatMemoryManager chatMemoryManager,
                          KnowledgeBaseManager knowledgeBaseManager,
                          ChatSessionRepository sessionRepository,
                          ChatMessageRepository messageRepository,
                          SessionTitleService sessionTitleService) {
        this.chatService = chatService;
        this.chatMemoryManager = chatMemoryManager;
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.sessionTitleService = sessionTitleService;
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

    @PostMapping("/upload")
    public String uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "❌ 文件为空！";
        }

        try {
            String fileName = file.getOriginalFilename();
            byte[] fileBytes = file.getBytes();
            knowledgeBaseManager.processPdfAsync(fileName, fileName, fileBytes);
            return "✅ 文件已交接给后台车间！";
        } catch (Exception e) {
            return "❌ 文件交接失败：" + e.getMessage();
        }
    }

    @GetMapping("/upload/status")
    public String getUploadStatus(@RequestParam("fileId") String fileId) {
        return knowledgeBaseManager.getStatus(fileId);
    }

    @GetMapping("/sessions")
    @ResponseBody
    public List<ChatSession> getSessions() {
        return sessionRepository.findAllByOrderByLastActiveTimeDescCreateTimeDesc();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @ResponseBody
    public List<ChatMessage> getMessages(@PathVariable String sessionId) {
        return messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    @PostMapping("/sessions")
    @ResponseBody
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

    @PutMapping("/sessions/{sessionId}/title")
    @ResponseBody
    public ChatSession renameSession(@PathVariable String sessionId,
                                     @Valid @RequestBody RenameSessionRequest request) {
        return sessionTitleService.renameSession(sessionId, request.title());
    }
}