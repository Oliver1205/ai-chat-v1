package com.example.ai_chat_v1.agent.studycoach.dto;

import java.util.List;

public record StudyCoachPlanDraft(
        String currentStateJudgment,
        List<String> keyProblemIdentification,
        List<StudyCoachTaskItem> taskSuggestions,
        List<String> priorityOrder,
        List<String> arrangementReason,
        String mainlineSummary
) implements StudyCoachDraft {
}
