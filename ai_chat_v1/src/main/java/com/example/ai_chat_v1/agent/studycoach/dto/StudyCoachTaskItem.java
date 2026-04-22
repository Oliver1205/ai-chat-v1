package com.example.ai_chat_v1.agent.studycoach.dto;

public record StudyCoachTaskItem(
        String timeScope,
        String priority,
        String title,
        String action,
        Integer durationMinutes,
        String expectedOutcome
) {
}
