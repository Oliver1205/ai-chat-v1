package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskItem;
import com.example.ai_chat_v1.agent.studycoach.evaluator.StudyCoachPlanEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyCoachPlanEvaluatorTest {

    private final StudyCoachPlanEvaluator evaluator = new StudyCoachPlanEvaluator();

    @Test
    void shouldDetectOverloadedPlan() {
        StudyCoachPlanDraft draft = new StudyCoachPlanDraft(
                "当前状态还行，但项目主线推进不足。",
                List.of("项目链路没形成完整表达", "复习内容有点发散"),
                List.of(
                        new StudyCoachTaskItem("TOMORROW", "P0", "项目主线梳理", "梳理项目背景、链路和亮点", 150, "形成一版项目表达提纲"),
                        new StudyCoachTaskItem("TOMORROW", "P1", "JVM 高频题复盘", "整理 5 道高频题答案", 120, "补一页错题总结"),
                        new StudyCoachTaskItem("TOMORROW", "P1", "算法补题", "做 2 道链表题", 90, "补一份题解"),
                        new StudyCoachTaskItem("TOMORROW", "P2", "JUC 笔记补充", "补线程池和 AQS 笔记", 90, "补完笔记")
                ),
                List.of("P0 项目主线梳理", "P1 JVM 高频题复盘", "P1 算法补题", "P2 JUC 笔记补充"),
                List.of("项目推进不足，需要拉回主线"),
                "先把项目主线拉回来。"
        );

        var result = evaluator.evaluatePlan("明天只有 4 小时，帮我安排一下。", draft);

        assertTrue(result.overloaded());
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void shouldPassCompactPlan() {
        StudyCoachPlanDraft draft = new StudyCoachPlanDraft(
                "当前主线明确，但项目推进节奏偏慢。",
                List.of("项目表达没有形成闭环", "知识复习抢占了项目时间"),
                List.of(
                        new StudyCoachTaskItem("TOMORROW", "P0", "项目表达主线梳理", "整理项目背景、核心链路、亮点和取舍", 90, "形成可复述的项目提纲"),
                        new StudyCoachTaskItem("TOMORROW", "P1", "JVM 错题复盘", "复盘 3 道 JVM 高频题并补答案", 60, "沉淀一页错题总结"),
                        new StudyCoachTaskItem("TOMORROW", "P1", "项目深挖补一段", "补项目中 Redis/MySQL 取舍的一段表达", 45, "补一段可直接面试回答的表达")
                ),
                List.of("P0 项目表达主线梳理", "P1 JVM 错题复盘", "P1 项目深挖补一段"),
                List.of("先确保项目主线推进，再做小块复习"),
                "项目主线优先，复习围绕面试表达补洞。"
        );

        var result = evaluator.evaluatePlan("明天只有 4 小时，帮我安排一下。", draft);

        assertTrue(result.specific());
        assertTrue(result.actionable());
        assertFalse(result.overloaded());
        assertTrue(result.priorityClear());
    }
}
