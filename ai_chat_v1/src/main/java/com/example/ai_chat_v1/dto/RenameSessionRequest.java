package com.example.ai_chat_v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameSessionRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 30, message = "标题不能超过30个字符")
        String title
) {
}