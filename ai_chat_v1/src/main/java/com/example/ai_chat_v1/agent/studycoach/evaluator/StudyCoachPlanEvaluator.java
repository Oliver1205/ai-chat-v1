package com.example.ai_chat_v1.agent.studycoach.evaluator;

import com.example.ai_chat_v1.agent.studycoach.StudyCoachTimeBudgetParser;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachExplanationDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanEvaluation;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class StudyCoachPlanEvaluator {

    public StudyCoachPlanEvaluation evaluatePlan(String originalQuestion, StudyCoachPlanDraft draft) {
        List<String> suggestions = new ArrayList<>();

        boolean hasState = hasText(draft.currentStateJudgment());
        boolean hasKeyProblems = draft.keyProblemIdentification() != null && !draft.keyProblemIdentification().isEmpty();
        boolean hasTasks = draft.taskSuggestions() != null && !draft.taskSuggestions().isEmpty();

        boolean specific = hasState && hasKeyProblems && hasTasks
                && draft.taskSuggestions().stream().allMatch(this::isTaskSpecific);

        boolean actionable = hasTasks && draft.taskSuggestions().stream().allMatch(this::isTaskActionable);

        Integer availableMinutes = StudyCoachTimeBudgetParser.parseMinutes(originalQuestion);
        int totalMinutes = draft.taskSuggestions() == null ? 0 : draft.taskSuggestions().stream()
                .map(StudyCoachTaskItem::durationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        boolean overloaded = false;
        if (availableMinutes != null && availableMinutes > 0) {
            overloaded = totalMinutes > Math.round(availableMinutes * 0.85)
                    || (availableMinutes <= 240 && draft.taskSuggestions().size() > 4);
        }

        boolean priorityClear = draft.priorityOrder() != null && !draft.priorityOrder().isEmpty()
                && draft.taskSuggestions().stream().allMatch(task -> hasText(task.priority()));

        if (!specific) {
            suggestions.add("把任务进一步具体到动作、时长和产出，不要只写大方向。");
        }
        if (!actionable) {
            suggestions.add("确保每个任务都包含明确动作和预期产出。");
        }
        if (overloaded) {
            suggestions.add("当前计划有过载风险，建议压缩到 2 到 4 个关键任务，并给项目主线留出完整时间块。");
        }
        if (!priorityClear) {
            suggestions.add("补充 P0 / P1 / P2 优先级，避免任务并列。");
        }

        String summary = suggestions.isEmpty()
                ? "计划具体、可执行，且节奏基本可控。"
                : "计划仍有可优化点：" + String.join("；", suggestions);

        return new StudyCoachPlanEvaluation(
                specific,
                actionable,
                overloaded,
                priorityClear,
                suggestions,
                summary
        );
    }

    public StudyCoachPlanEvaluation evaluateExplanation(StudyCoachExplanationDraft draft) {
        List<String> suggestions = new ArrayList<>();

        boolean specific = hasText(draft.questionEssence())
                && hasText(draft.whyHardToUnderstand())
                && hasText(draft.plainExplanation());

        boolean actionable = hasText(draft.stageAdvice())
                && hasText(draft.nextGapToLearn());

        if (!specific) {
            suggestions.add("补足问题本质、难点来源和通俗解释。");
        }
        if (!actionable) {
            suggestions.add("补充结合当前阶段的掌握建议和下一步补强方向。");
        }

        String summary = suggestions.isEmpty()
                ? "解释结构完整，能直接用于当前阶段学习。"
                : "解释仍需补足：" + String.join("；", suggestions);

        return new StudyCoachPlanEvaluation(
                specific,
                actionable,
                false,
                true,
                suggestions,
                summary
        );
    }

    private boolean isTaskSpecific(StudyCoachTaskItem task) {
        return hasText(task.title())
                && hasText(task.action())
                && task.durationMinutes() != null
                && task.durationMinutes() > 0;
    }

    private boolean isTaskActionable(StudyCoachTaskItem task) {
        return hasText(task.action()) && hasText(task.expectedOutcome());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
