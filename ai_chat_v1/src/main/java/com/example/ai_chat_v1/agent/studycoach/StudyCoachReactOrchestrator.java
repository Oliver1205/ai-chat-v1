package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.LearningKnowledgeSearchRequest;
import com.example.ai_chat_v1.agent.studycoach.dto.LearningKnowledgeSearchResult;
import com.example.ai_chat_v1.agent.studycoach.dto.MemoryReadRequest;
import com.example.ai_chat_v1.agent.studycoach.dto.MemoryReadResult;
import com.example.ai_chat_v1.agent.studycoach.dto.PlanEvaluateRequest;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanEvaluation;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachReactDecision;
import com.example.ai_chat_v1.agent.studycoach.tool.LearningKnowledgeSearchTool;
import com.example.ai_chat_v1.agent.studycoach.tool.MemoryReadTool;
import com.example.ai_chat_v1.agent.studycoach.tool.PlanEvaluateTool;
import org.springframework.stereotype.Component;

@Component
public class StudyCoachReactOrchestrator {

    private final StudyCoachIntentAnalyzer intentAnalyzer;
    private final MemoryReadTool memoryReadTool;
    private final LearningKnowledgeSearchTool learningKnowledgeSearchTool;
    private final StudyCoachDraftGenerator draftGenerator;
    private final PlanEvaluateTool planEvaluateTool;
    private final StudyCoachResponseFormatter responseFormatter;

    public StudyCoachReactOrchestrator(StudyCoachIntentAnalyzer intentAnalyzer,
                                       MemoryReadTool memoryReadTool,
                                       LearningKnowledgeSearchTool learningKnowledgeSearchTool,
                                       StudyCoachDraftGenerator draftGenerator,
                                       PlanEvaluateTool planEvaluateTool,
                                       StudyCoachResponseFormatter responseFormatter) {
        this.intentAnalyzer = intentAnalyzer;
        this.memoryReadTool = memoryReadTool;
        this.learningKnowledgeSearchTool = learningKnowledgeSearchTool;
        this.draftGenerator = draftGenerator;
        this.planEvaluateTool = planEvaluateTool;
        this.responseFormatter = responseFormatter;
    }

    public String execute(String sessionId, String userMessage) {
        StudyCoachReactDecision decision = intentAnalyzer.analyze(userMessage);

        MemoryReadResult memoryResult = decision.shouldReadMemory()
                ? memoryReadTool.execute(new MemoryReadRequest(sessionId, userMessage, decision.taskType()))
                : MemoryReadResult.empty();

        LearningKnowledgeSearchResult knowledgeResult = decision.shouldSearchKnowledge()
                ? learningKnowledgeSearchTool.execute(new LearningKnowledgeSearchRequest(userMessage, decision.taskType()))
                : LearningKnowledgeSearchResult.empty();

        StudyCoachDraft draft = draftGenerator.generate(userMessage, decision, memoryResult, knowledgeResult);

        if (decision.shouldEvaluate()) {
            StudyCoachPlanEvaluation evaluation = planEvaluateTool.execute(
                    new PlanEvaluateRequest(decision.taskType(), userMessage, draft)
            );

            if (evaluation.needsRevision()) {
                draft = draftGenerator.revise(userMessage, decision, memoryResult, knowledgeResult, draft, evaluation);
            }
        }

        return responseFormatter.format(draft);
    }
}
