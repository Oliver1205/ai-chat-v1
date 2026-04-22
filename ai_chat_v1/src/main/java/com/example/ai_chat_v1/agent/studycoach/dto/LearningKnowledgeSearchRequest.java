package com.example.ai_chat_v1.agent.studycoach.dto;

public record LearningKnowledgeSearchRequest(
        String query,
        StudyCoachTaskType taskType
) {
}
