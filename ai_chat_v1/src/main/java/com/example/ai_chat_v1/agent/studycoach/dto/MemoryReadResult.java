package com.example.ai_chat_v1.agent.studycoach.dto;

import java.util.List;

public record MemoryReadResult(
        String summary,
        List<String> hits
) {

    public static MemoryReadResult empty() {
        return new MemoryReadResult("", List.of());
    }

    public boolean hasContent() {
        return summary != null && !summary.isBlank();
    }
}
