package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyCoachIntentAnalyzerTest {

    private final StudyCoachIntentAnalyzer analyzer = new StudyCoachIntentAnalyzer();

    @Test
    void shouldClassifyExplanationQuestion() {
        var decision = analyzer.analyze("我还是搞不懂 Redis 和 MySQL 双写一致性，你结合我当前学习阶段给我解释一下。");

        assertEquals(StudyCoachTaskType.EXPLANATION, decision.taskType());
        assertTrue(decision.shouldSearchKnowledge());
    }

    @Test
    void shouldClassifyReviewQuestion() {
        var decision = analyzer.analyze("我今天刷了 3 道 JVM 题，项目没推进，帮我复盘一下今天的学习情况。");

        assertEquals(StudyCoachTaskType.REVIEW, decision.taskType());
        assertTrue(decision.shouldReadMemory());
    }

    @Test
    void shouldClassifyPlanQuestion() {
        var decision = analyzer.analyze("我这两天主要学了 JVM 和 JUC，但感觉项目推进不够，明天只有 4 小时，帮我安排一下。");

        assertEquals(StudyCoachTaskType.PLAN, decision.taskType());
        assertTrue(decision.shouldEvaluate());
    }
}
