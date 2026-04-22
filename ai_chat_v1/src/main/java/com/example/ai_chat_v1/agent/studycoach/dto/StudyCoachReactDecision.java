package com.example.ai_chat_v1.agent.studycoach.dto;

public record StudyCoachReactDecision(
        StudyCoachTaskType taskType,
        boolean shouldReadMemory,
        boolean shouldSearchKnowledge,
        boolean shouldEvaluate,
        String reason
) {
}
