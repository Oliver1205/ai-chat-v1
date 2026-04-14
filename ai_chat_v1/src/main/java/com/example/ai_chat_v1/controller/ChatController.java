package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.entity.ChatSession;
import com.example.ai_chat_v1.repository.ChatMessageRepository;
import com.example.ai_chat_v1.repository.ChatSessionRepository;
import com.example.ai_chat_v1.service.ChatMemoryManager;
import com.example.ai_chat_v1.service.KnowledgeBaseManager;
import com.example.ai_chat_v1.service.LlmChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    // 👇 1. 所有的依赖全部整齐地排在类的最顶部
    private final LlmChatService chatService;
    private final ChatMemoryManager chatMemoryManager;
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    // 👇 2. 超级构造器：一次性安全地注入所有小弟
    public ChatController(LlmChatService chatService,
                          ChatMemoryManager chatMemoryManager,
                          KnowledgeBaseManager knowledgeBaseManager,
                          ChatSessionRepository sessionRepository,
                          ChatMessageRepository messageRepository) {
        this.chatService = chatService;
        this.chatMemoryManager = chatMemoryManager;
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    // --- 核心接口 1：流式对话 ---
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestParam(defaultValue = "default-user") String sessionId,
            @RequestParam(defaultValue = "你好") String message) {

        SseEmitter emitter = new SseEmitter(60000L);
        chatService.streamChat(
                sessionId, message,
                token -> {
                    try { emitter.send(token); } catch (IOException e) { emitter.completeWithError(e); }
                },
                emitter::complete,
                emitter::completeWithError
        );
        return emitter;
    }

    // --- 核心接口 2：清空记忆 ---
    @GetMapping("/clear")
    public String clearMemory(@RequestParam(defaultValue = "default-user") String sessionId) {
        chatMemoryManager.clear(sessionId);
        return "✅ 用户 [" + sessionId + "] 的记忆已成功清空！可以开始全新的对话了。";
    }

    // --- 新增接口 3：接收 PDF 文件上传 ---
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

    // --- 新增接口 4：查询文件处理进度 ---
    @GetMapping("/upload/status")
    public String getUploadStatus(@RequestParam("fileId") String fileId) {
        return knowledgeBaseManager.getStatus(fileId);
    }

    // --- 新增接口 5：获取左侧边栏的会话列表 ---
    @GetMapping("/sessions")
    @ResponseBody
    public List<ChatSession> getSessions() {
        return sessionRepository.findAllByOrderByCreateTimeDesc();
    }

    // --- 新增接口 6：获取右侧聊天框的详细记录 ---
    @GetMapping("/sessions/{sessionId}/messages")
    @ResponseBody
    public List<ChatMessage> getMessages(@PathVariable String sessionId) {
        return messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    // --- 新增接口 7：新建一个空白会话 ---
    @PostMapping("/sessions")
    @ResponseBody
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setTitle("新对话 " + DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()));
        session.setCreateTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }
}