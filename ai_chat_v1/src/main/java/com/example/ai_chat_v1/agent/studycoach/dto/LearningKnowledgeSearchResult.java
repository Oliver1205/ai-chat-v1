package com.example.ai_chat_v1.agent.studycoach.dto;

import java.util.List;

public record LearningKnowledgeSearchResult(
        String summary,
        List<String> fragments
) {

    public static LearningKnowledgeSearchResult empty() {
        return new LearningKnowledgeSearchResult("", List.of());
    }

    public boolean hasContent() {
        return summary != null && !summary.isBlank();
    }
}
