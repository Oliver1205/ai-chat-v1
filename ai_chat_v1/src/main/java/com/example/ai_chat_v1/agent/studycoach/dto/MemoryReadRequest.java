package com.example.ai_chat_v1.agent.studycoach.dto;

public record MemoryReadRequest(
        String sessionId,
        String currentQuestion,
        StudyCoachTaskType taskType
) {
}
