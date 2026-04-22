package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachExplanationDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudyCoachResponseFormatter {

    public String format(StudyCoachDraft draft) {
        if (draft instanceof StudyCoachExplanationDraft explanationDraft) {
            return formatExplanation(explanationDraft);
        }
        if (draft instanceof StudyCoachPlanDraft planDraft) {
            return formatPlan(planDraft);
        }
        throw new IllegalStateException("Unsupported draft type: " + draft.getClass().getName());
    }

    private String formatPlan(StudyCoachPlanDraft draft) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "1. 当前状态判断", valueOrFallback(draft.currentStateJudgment(), "暂无稳定判断。"));
        appendSection(sb, "2. 关键问题识别", formatList(draft.keyProblemIdentification(), "暂无明确问题，先从主线推进卡点排查。"));
        appendSection(sb, "3. 今日 / 明日学习任务建议", formatTasks(draft.taskSuggestions()));
        appendSection(sb, "4. 优先级排序", formatList(draft.priorityOrder(), "P0 先把当前主线任务做实。"));
        appendSection(sb, "5. 为什么这么安排", formatList(draft.arrangementReason(), "优先保证主线推进和短周期反馈。"));
        appendSection(sb, "6. 一句主线总结", valueOrFallback(draft.mainlineSummary(), "先收敛主线，再做扩展。"));
        return sb.toString().trim();
    }

    private String formatExplanation(StudyCoachExplanationDraft draft) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "1. 问题本质", valueOrFallback(draft.questionEssence(), "先明确这个知识点要解决什么问题。"));
        appendSection(sb, "2. 为什么难理解", valueOrFallback(draft.whyHardToUnderstand(), "它同时涉及多个概念，容易把场景、手段和结果混在一起。"));
        appendSection(sb, "3. 通俗解释", valueOrFallback(draft.plainExplanation(), "先从最核心的业务冲突和解决手段理解。"));
        appendSection(sb, "4. 结合当前学习阶段的理解建议", valueOrFallback(draft.stageAdvice(), "先掌握面试和项目表达层面的主线，不必一开始追求极端细节。"));
        appendSection(sb, "5. 下一步该补什么", valueOrFallback(draft.nextGapToLearn(), "补标准定义、典型方案和项目里的取舍理由。"));
        return sb.toString().trim();
    }

    private String formatTasks(List<StudyCoachTaskItem> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "- 暂无任务，先补一个 60 分钟的主线整理任务。";
        }

        StringBuilder sb = new StringBuilder();
        for (StudyCoachTaskItem task : tasks) {
            sb.append("- ")
                    .append(valueOrFallback(normalizeTimeScope(task.timeScope()), "明日"))
                    .append(" ")
                    .append(valueOrFallback(task.priority(), "P1"))
                    .append(" | ")
                    .append(valueOrFallback(task.title(), "未命名任务"))
                    .append(" | ")
                    .append(task.durationMinutes() == null ? "时长待补" : task.durationMinutes() + " 分钟")
                    .append("\n")
                    .append("  行动：")
                    .append(valueOrFallback(task.action(), "补具体动作"))
                    .append("\n")
                    .append("  产出：")
                    .append(valueOrFallback(task.expectedOutcome(), "补预期产出"))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String formatList(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) {
            return "- " + fallback;
        }

        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.toString().trim();
    }

    private String normalizeTimeScope(String value) {
        if (value == null || value.isBlank()) {
            return "明日";
        }
        if ("TODAY".equalsIgnoreCase(value) || value.contains("今日") || value.contains("今天")) {
            return "今日";
        }
        return "明日";
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void appendSection(StringBuilder sb, String title, String content) {
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(title).append("\n").append(content);
    }
}
