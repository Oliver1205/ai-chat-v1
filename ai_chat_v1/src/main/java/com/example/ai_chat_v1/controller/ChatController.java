package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.service.ChatMemoryManager;
import com.example.ai_chat_v1.service.KnowledgeBaseManager;
import com.example.ai_chat_v1.service.LlmChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final LlmChatService chatService;
    private final ChatMemoryManager chatMemoryManager;
    // 👇 新增：把知识库管理员也请到前台来
    private final KnowledgeBaseManager knowledgeBaseManager;

    // 构造器注入（加入了 knowledgeBaseManager）
    public ChatController(LlmChatService chatService, ChatMemoryManager chatMemoryManager, KnowledgeBaseManager knowledgeBaseManager) {
        this.chatService = chatService;
        this.chatMemoryManager = chatMemoryManager;
        this.knowledgeBaseManager = knowledgeBaseManager;
    }

    // --- 核心接口 1：流式对话（保持不变） ---
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

    // --- 核心接口 2：清空记忆（保持不变） ---
    @GetMapping("/clear")
    public String clearMemory(@RequestParam(defaultValue = "default-user") String sessionId) {
        chatMemoryManager.clear(sessionId);
        return "✅ 用户 [" + sessionId + "] 的记忆已成功清空！可以开始全新的对话了。";
    }

    // 👇 新增接口 3：接收 PDF 文件上传
    @PostMapping("/upload")
    public String uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "❌ 文件为空！";
        }
        try {
            String fileName = file.getOriginalFilename();
            // 极其核心：在这里把文件转成 byte[]，摆脱临时文件的束缚！
            byte[] fileBytes = file.getBytes();

            // 呼叫后台隐形工人去干活（这行代码瞬间执行完，不会卡住）
            knowledgeBaseManager.processPdfAsync(fileName, fileName, fileBytes);

            return "✅ 文件已交接给后台车间！";
        } catch (Exception e) {
            return "❌ 文件交接失败：" + e.getMessage();
        }
    }

    // 👇 新增接口 4：查询文件处理进度
    @GetMapping("/upload/status")
    public String getUploadStatus(@RequestParam("fileId") String fileId) {
        return knowledgeBaseManager.getStatus(fileId);
    }
}