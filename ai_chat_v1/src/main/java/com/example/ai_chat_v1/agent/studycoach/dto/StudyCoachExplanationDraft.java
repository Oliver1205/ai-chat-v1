package com.example.ai_chat_v1.agent.studycoach.dto;

public record StudyCoachExplanationDraft(
        String questionEssence,
        String whyHardToUnderstand,
        String plainExplanation,
        String stageAdvice,
        String nextGapToLearn
) implements StudyCoachDraft {
}
