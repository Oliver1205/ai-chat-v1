package com.example.ai_chat_v1.agent.studycoach.dto;

public record PlanEvaluateRequest(
        StudyCoachTaskType taskType,
        String originalQuestion,
        StudyCoachDraft draft
) {
}
