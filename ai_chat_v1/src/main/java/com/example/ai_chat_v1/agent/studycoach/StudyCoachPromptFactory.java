package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.LearningKnowledgeSearchResult;
import com.example.ai_chat_v1.agent.studycoach.dto.MemoryReadResult;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachExplanationDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanEvaluation;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachReactDecision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class StudyCoachPromptFactory {

    private final ObjectMapper objectMapper;

    public StudyCoachPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildDraftPrompt(String userMessage,
                                   StudyCoachReactDecision decision,
                                   MemoryReadResult memory,
                                   LearningKnowledgeSearchResult knowledge) {
        Integer availableMinutes = StudyCoachTimeBudgetParser.parseMinutes(userMessage);

        return """
                你是 StudyCoachAgent，一个学习教练型任务 Agent。
                你的职责只允许聚焦：
                1. 学习规划
                2. 学习复盘
                3. 根据完成情况动态调整下一步
                4. 针对疑难知识点做辅助解释

                禁止事项：
                - 不要鸡汤
                - 不要泛化成万能助手
                - 不要输出与学习推进无关的空话
                - 不要编造资料来源

                当前任务类型：%s
                ReAct 判断理由：%s
                用户问题：%s
                可投入时间（分钟，若未提及则为空）：%s

                历史学习上下文：
                %s

                检索到的学习资料片段：
                %s

                输出要求：
                - 如果任务类型是 PLAN 或 REVIEW，只能输出 JSON，对应 schema 为：
                  {
                    "responseType": "PLAN",
                    "currentStateJudgment": "...",
                    "keyProblemIdentification": ["...", "..."],
                    "taskSuggestions": [
                      {
                        "timeScope": "TODAY 或 TOMORROW",
                        "priority": "P0/P1/P2",
                        "title": "...",
                        "action": "...",
                        "durationMinutes": 60,
                        "expectedOutcome": "..."
                      }
                    ],
                    "priorityOrder": ["P0 ...", "P1 ..."],
                    "arrangementReason": ["...", "..."],
                    "mainlineSummary": "..."
                  }
                - 如果任务类型是 EXPLANATION，只能输出 JSON，对应 schema 为：
                  {
                    "responseType": "EXPLANATION",
                    "questionEssence": "...",
                    "whyHardToUnderstand": "...",
                    "plainExplanation": "...",
                    "stageAdvice": "...",
                    "nextGapToLearn": "..."
                  }

                强约束：
                - 计划类输出必须具体、可执行，任务总数控制在 2 到 4 个
                - 如果给出了可投入时间，总时长不要超过 85%%
                - 任务优先级必须清晰，项目推进不足时优先补项目链路
                - 知识点解释必须结合“当前学习阶段应该掌握到什么程度”
                - 只输出 JSON，不要 markdown，不要代码块，不要额外解释
                """.formatted(
                decision.taskType().name(),
                safe(decision.reason()),
                safe(userMessage),
                availableMinutes == null ? "" : availableMinutes,
                blockOrFallback(memory.summary(), "暂无明显相关历史学习记录。"),
                blockOrFallback(knowledge.summary(), "暂无明显相关知识库片段。")
        );
    }

    public String buildRevisionPrompt(String userMessage,
                                      StudyCoachReactDecision decision,
                                      MemoryReadResult memory,
                                      LearningKnowledgeSearchResult knowledge,
                                      StudyCoachDraft draft,
                                      StudyCoachPlanEvaluation evaluation) {
        return """
                你是 StudyCoachAgent，现在需要修正一个已经生成的草稿。
                任务类型：%s
                用户问题：%s

                历史学习上下文：
                %s

                检索到的学习资料片段：
                %s

                当前草稿 JSON：
                %s

                评估结果：
                - 是否具体：%s
                - 是否可执行：%s
                - 是否过载：%s
                - 是否优先级清晰：%s
                - 修正建议：%s

                请在不改变任务类型和输出 schema 的前提下，重新输出一份更好的 JSON。
                要求仍然是：
                - 只输出 JSON
                - 计划任务控制在 2 到 4 个
                - 用户给出时间预算时，总时长不要超过 85%%
                - 继续结合历史主线，不要只追着表层问题跑
                """.formatted(
                decision.taskType().name(),
                safe(userMessage),
                blockOrFallback(memory.summary(), "暂无明显相关历史学习记录。"),
                blockOrFallback(knowledge.summary(), "暂无明显相关知识库片段。"),
                serializeDraft(draft),
                evaluation.specific(),
                evaluation.actionable(),
                evaluation.overloaded(),
                evaluation.priorityClear(),
                String.join("；", evaluation.suggestions())
        );
    }

    private String serializeDraft(StudyCoachDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            if (draft instanceof StudyCoachPlanDraft planDraft) {
                return planDraft.toString();
            }
            if (draft instanceof StudyCoachExplanationDraft explanationDraft) {
                return explanationDraft.toString();
            }
            return draft.toString();
        }
    }

    private String blockOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
