package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.agent.studycoach.dto.LearningKnowledgeSearchResult;
import com.example.ai_chat_v1.agent.studycoach.dto.MemoryReadResult;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachExplanationDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanDraft;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachPlanEvaluation;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachReactDecision;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskItem;
import com.example.ai_chat_v1.agent.studycoach.dto.StudyCoachTaskType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StudyCoachDraftGenerator {

    private final StudyCoachTextGenerator textGenerator;
    private final StudyCoachPromptFactory promptFactory;
    private final ObjectMapper objectMapper;

    public StudyCoachDraftGenerator(StudyCoachTextGenerator textGenerator,
                                    StudyCoachPromptFactory promptFactory,
                                    ObjectMapper objectMapper) {
        this.textGenerator = textGenerator;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
    }

    public StudyCoachDraft generate(String userMessage,
                                    StudyCoachReactDecision decision,
                                    MemoryReadResult memory,
                                    LearningKnowledgeSearchResult knowledge) {
        String prompt = promptFactory.buildDraftPrompt(userMessage, decision, memory, knowledge);
        String raw = textGenerator.generate(prompt);
        return parseDraft(raw, decision.taskType());
    }

    public StudyCoachDraft revise(String userMessage,
                                  StudyCoachReactDecision decision,
                                  MemoryReadResult memory,
                                  LearningKnowledgeSearchResult knowledge,
                                  StudyCoachDraft originalDraft,
                                  StudyCoachPlanEvaluation evaluation) {
        String prompt = promptFactory.buildRevisionPrompt(
                userMessage,
                decision,
                memory,
                knowledge,
                originalDraft,
                evaluation
        );
        String raw = textGenerator.generate(prompt);
        return parseDraft(raw, decision.taskType());
    }

    private StudyCoachDraft parseDraft(String raw, StudyCoachTaskType taskType) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            if (taskType == StudyCoachTaskType.EXPLANATION || "EXPLANATION".equalsIgnoreCase(text(root, "responseType"))) {
                return new StudyCoachExplanationDraft(
                        text(root, "questionEssence"),
                        text(root, "whyHardToUnderstand"),
                        text(root, "plainExplanation"),
                        text(root, "stageAdvice"),
                        text(root, "nextGapToLearn")
                );
            }

            return new StudyCoachPlanDraft(
                    text(root, "currentStateJudgment"),
                    readStringList(root, "keyProblemIdentification"),
                    readTaskSuggestions(root.path("taskSuggestions")),
                    readStringList(root, "priorityOrder"),
                    readStringList(root, "arrangementReason"),
                    text(root, "mainlineSummary")
            );
        } catch (Exception e) {
            if (taskType == StudyCoachTaskType.EXPLANATION) {
                return new StudyCoachExplanationDraft(
                        "核心问题未能稳定解析",
                        "模型返回了非结构化内容",
                        raw == null ? "" : raw.trim(),
                        "先按返回内容抓住核心因果，再补标准概念边界。",
                        "补一份当前主题的标准定义和典型场景。"
                );
            }

            return new StudyCoachPlanDraft(
                    "当前问题可初步判断为需要重新聚焦主线。",
                    List.of("模型返回了非结构化草稿，已降级为保底计划。"),
                    List.of(
                            new StudyCoachTaskItem("TOMORROW", "P0", "重写明日主线任务", "先梳理明天最重要的一件事，再拆成两个执行动作。", 60, "得到可执行的二段式计划")
                    ),
                    List.of("P0 先确定主线任务"),
                    List.of("当前草稿不够稳定，先保证下一步有明确动作。"),
                    "先收敛主线，再继续扩展。"
            );
        }
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Study coach raw response is blank");
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Study coach response does not contain JSON object");
        }

        return trimmed.substring(start, end + 1);
    }

    private List<String> readStringList(JsonNode root, String fieldName) {
        JsonNode arrayNode = root.path(fieldName);
        if (!arrayNode.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String value = node.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private List<StudyCoachTaskItem> readTaskSuggestions(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }

        List<StudyCoachTaskItem> items = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            Integer durationMinutes = node.has("durationMinutes") && node.get("durationMinutes").canConvertToInt()
                    ? node.get("durationMinutes").asInt()
                    : null;

            items.add(new StudyCoachTaskItem(
                    text(node, "timeScope"),
                    text(node, "priority"),
                    text(node, "title"),
                    text(node, "action"),
                    durationMinutes,
                    text(node, "expectedOutcome")
            ));
        }
        return items;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        return valueNode.isMissingNode() || valueNode.isNull() ? "" : valueNode.asText("").trim();
    }
}
