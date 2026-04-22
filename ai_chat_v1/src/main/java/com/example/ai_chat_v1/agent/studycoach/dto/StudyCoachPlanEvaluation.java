package com.example.ai_chat_v1.agent.studycoach.dto;

import java.util.List;

public record StudyCoachPlanEvaluation(
        boolean specific,
        boolean actionable,
        boolean overloaded,
        boolean priorityClear,
        List<String> suggestions,
        String summary
) {

    public boolean needsRevision() {
        return !specific || !actionable || overloaded || !priorityClear;
    }
}
