package com.example.ai_chat_v1.agent.studycoach.tool;

import com.example.ai_chat_v1.agent.studycoach.dto.PlanEvaluateRequest;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachExplanationDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanEvaluation;
import com.example.ai_chat_v1.agent.studycoach.evaluator.StudyCoachPlanEvaluator;
import org.springframework.stereotype.Component;

@Component
public class PlanEvaluateTool {

    private final StudyCoachPlanEvaluator planEvaluator;

    public PlanEvaluateTool(StudyCoachPlanEvaluator planEvaluator) {
        this.planEvaluator = planEvaluator;
    }

    public StudyCoachPlanEvaluation execute(PlanEvaluateRequest request) {
        if (request.draft() instanceof StudyCoachExplanationDraft explanationDraft) {
            return planEvaluator.evaluateExplanation(explanationDraft);
        }

        if (request.draft() instanceof StudyCoachPlanDraft planDraft) {
            return planEvaluator.evaluatePlan(request.originalQuestion(), planDraft);
        }

        throw new IllegalStateException("Unsupported study coach draft type: " + request.draft().getClass().getName());
    }
}
