package com.example.ai_chat_v1.agent.studycoach.access;

import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StudyCoachMemoryAccessService {

    private static final int MAX_HITS = 6;
    private static final int MAX_TEXT_LENGTH = 120;
    private static final Pattern ENGLISH_TOKEN_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+#\\-]{1,}");
    private static final List<String> DOMAIN_KEYWORDS = List.of(
            "JVM", "JUC", "Redis", "MySQL", "MQ", "Spring", "项目", "算法", "面试", "复盘",
            "计划", "学习", "主线", "一致性", "双写", "缓存", "线程", "并发", "八股", "知识点"
    );

    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public StudyCoachMemoryAccessService(ChatMessageRepository chatMessageRepository,
                                         ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    public List<String> readRelevantMemoryLines(String sessionId, String currentQuestion) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }

        List<ReadableMessage> readableMessages = new ArrayList<>();
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        for (int i = 0; i < messages.size(); i++) {
            ReadableMessage readableMessage = toReadableMessage(messages.get(i), i);
            if (readableMessage == null || readableMessage.text().isBlank()) {
                continue;
            }
            readableMessages.add(readableMessage);
        }

        if (readableMessages.isEmpty()) {
            return List.of();
        }

        List<String> keywords = extractKeywords(currentQuestion);
        List<ScoredMessage> scoredMessages = new ArrayList<>();

        for (int i = 0; i < readableMessages.size(); i++) {
            ReadableMessage message = readableMessages.get(i);
            int recencyBoost = i;
            int score = scoreMessage(message.text(), keywords, recencyBoost);
            if (score > 0) {
                scoredMessages.add(new ScoredMessage(message, score));
            }
        }

        if (scoredMessages.isEmpty()) {
            return readableMessages.stream()
                    .skip(Math.max(0, readableMessages.size() - MAX_HITS))
                    .map(this::formatMemoryLine)
                    .toList();
        }

        return scoredMessages.stream()
                .sorted(Comparator.comparingInt(ScoredMessage::score).reversed()
                        .thenComparing(sm -> sm.message().index(), Comparator.reverseOrder()))
                .limit(MAX_HITS)
                .map(ScoredMessage::message)
                .sorted(Comparator.comparingInt(ReadableMessage::index))
                .map(this::formatMemoryLine)
                .toList();
    }

    private ReadableMessage toReadableMessage(ChatMessage message, int index) {
        String normalizedRole = normalizeRole(message.getRole());
        if (normalizedRole == null) {
            return null;
        }

        String text = extractReadableText(message.getContent());
        if (text.isBlank()) {
            return null;
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH) + "...";
        }

        return new ReadableMessage(index, normalizedRole, text);
    }

    private String normalizeRole(String role) {
        if ("USER".equalsIgnoreCase(role)) {
            return "用户";
        }
        if ("AI".equalsIgnoreCase(role)) {
            return "助理";
        }
        return null;
    }

    private String extractReadableText(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(rawContent);
            String text = "";

            if (root.has("text") && !root.get("text").isNull()) {
                text = root.get("text").asText("");
            } else if (root.has("contents")
                    && root.get("contents").isArray()
                    && !root.get("contents").isEmpty()
                    && root.get("contents").get(0).has("text")) {
                text = root.get("contents").get(0).get("text").asText("");
            }

            return cleanupText(text);
        } catch (Exception e) {
            return cleanupText(rawContent);
        }
    }

    private String cleanupText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text.trim();
        int questionIndex = cleaned.indexOf("【我的问题】");
        if (questionIndex >= 0) {
            cleaned = cleaned.substring(questionIndex + "【我的问题】".length()).trim();
        }

        return cleaned.replaceAll("\\s+", " ");
    }

    private List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();

        for (String keyword : DOMAIN_KEYWORDS) {
            if (question.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                keywords.add(keyword);
            }
        }

        Matcher matcher = ENGLISH_TOKEN_PATTERN.matcher(question);
        while (matcher.find()) {
            keywords.add(matcher.group());
        }

        return new ArrayList<>(keywords);
    }

    private int scoreMessage(String text, List<String> keywords, int recencyBoost) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        int score = Math.max(1, recencyBoost / 3);
        for (String keyword : keywords) {
            if (text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                score += 4;
            }
        }
        return score;
    }

    private String formatMemoryLine(ReadableMessage readableMessage) {
        return readableMessage.role() + "：" + readableMessage.text();
    }

    private record ReadableMessage(int index, String role, String text) {
    }

    private record ScoredMessage(ReadableMessage message, int score) {
    }
}
