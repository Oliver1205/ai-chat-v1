package com.example.ai_chat_v1.controller;

import com.example.ai_chat_v1.service.KnowledgeBaseManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class KnowledgeController {

    private final KnowledgeBaseManager knowledgeBaseManager;

    public KnowledgeController(KnowledgeBaseManager knowledgeBaseManager) {
        this.knowledgeBaseManager = knowledgeBaseManager;
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
}