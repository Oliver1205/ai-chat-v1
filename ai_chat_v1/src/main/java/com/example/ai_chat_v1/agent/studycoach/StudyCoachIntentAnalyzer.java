package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachReactDecision;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudyCoachIntentAnalyzer {

    private static final List<String> EXPLANATION_KEYWORDS = List.of(
            "搞不懂", "不懂", "解释", "理解", "为什么", "原理", "双写一致性", "知识点", "怎么理解"
    );
    private static final List<String> REVIEW_KEYWORDS = List.of(
            "复盘", "总结今天", "今天学了", "今天刷了", "帮我复盘", "状态一般", "完成情况"
    );
    private static final List<String> KNOWLEDGE_KEYWORDS = List.of(
            "JVM", "JUC", "Redis", "MySQL", "MQ", "项目", "面试", "资料", "复盘", "日志", "PDF"
    );

    public StudyCoachReactDecision analyze(String userMessage) {
        StudyCoachTaskType taskType = classifyTaskType(userMessage);
        boolean shouldReadMemory = true;
        boolean shouldSearchKnowledge = taskType == StudyCoachTaskType.EXPLANATION || containsAny(userMessage, KNOWLEDGE_KEYWORDS);
        boolean shouldEvaluate = true;

        String reason = switch (taskType) {
            case PLAN -> "用户在请求学习规划或下一步安排，优先读取历史主线并评估计划负载。";
            case REVIEW -> "用户在请求学习复盘，需要先看近期学习记录，再给调整建议。";
            case EXPLANATION -> "用户在追问知识点理解，需要结合当前阶段和知识库做解释。";
        };

        return new StudyCoachReactDecision(taskType, shouldReadMemory, shouldSearchKnowledge, shouldEvaluate, reason);
    }

    private StudyCoachTaskType classifyTaskType(String userMessage) {
        if (containsAny(userMessage, EXPLANATION_KEYWORDS)) {
            return StudyCoachTaskType.EXPLANATION;
        }
        if (containsAny(userMessage, REVIEW_KEYWORDS)) {
            return StudyCoachTaskType.REVIEW;
        }
        return StudyCoachTaskType.PLAN;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return keywords.stream().anyMatch(text::contains);
    }
}
