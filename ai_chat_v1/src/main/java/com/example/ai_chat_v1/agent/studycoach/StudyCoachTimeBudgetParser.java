package com.example.ai_chat_v1.agent.studycoach;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudyCoachTimeBudgetParser {

    private static final Pattern ARABIC_HOURS = Pattern.compile("(\\d+)\\s*(?:个)?\\s*(?:小时|h|H)");
    private static final Pattern ARABIC_MINUTES = Pattern.compile("(\\d+)\\s*(?:分钟|min)");
    private static final Pattern CHINESE_HOURS = Pattern.compile("([一二两三四五六七八九十]+)\\s*(?:个)?\\s*小时");
    private static final Pattern CHINESE_MINUTES = Pattern.compile("([一二两三四五六七八九十]+)\\s*分钟");

    private static final Map<Character, Integer> CHINESE_NUMBER_MAP = Map.ofEntries(
            Map.entry('一', 1),
            Map.entry('二', 2),
            Map.entry('两', 2),
            Map.entry('三', 3),
            Map.entry('四', 4),
            Map.entry('五', 5),
            Map.entry('六', 6),
            Map.entry('七', 7),
            Map.entry('八', 8),
            Map.entry('九', 9),
            Map.entry('十', 10)
    );

    private StudyCoachTimeBudgetParser() {
    }

    public static Integer parseMinutes(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int totalMinutes = 0;
        boolean matched = false;

        Matcher arabicHours = ARABIC_HOURS.matcher(text);
        while (arabicHours.find()) {
            totalMinutes += Integer.parseInt(arabicHours.group(1)) * 60;
            matched = true;
        }

        Matcher arabicMinutes = ARABIC_MINUTES.matcher(text);
        while (arabicMinutes.find()) {
            totalMinutes += Integer.parseInt(arabicMinutes.group(1));
            matched = true;
        }

        Matcher chineseHours = CHINESE_HOURS.matcher(text);
        while (chineseHours.find()) {
            totalMinutes += parseChineseNumber(chineseHours.group(1)) * 60;
            matched = true;
        }

        Matcher chineseMinutes = CHINESE_MINUTES.matcher(text);
        while (chineseMinutes.find()) {
            totalMinutes += parseChineseNumber(chineseMinutes.group(1));
            matched = true;
        }

        return matched ? totalMinutes : null;
    }

    private static int parseChineseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        if ("十".equals(value)) {
            return 10;
        }

        if (value.length() == 2 && value.charAt(0) == '十') {
            return 10 + CHINESE_NUMBER_MAP.getOrDefault(value.charAt(1), 0);
        }

        if (value.length() == 2 && value.charAt(1) == '十') {
            return CHINESE_NUMBER_MAP.getOrDefault(value.charAt(0), 0) * 10;
        }

        if (value.length() == 3 && value.charAt(1) == '十') {
            return CHINESE_NUMBER_MAP.getOrDefault(value.charAt(0), 0) * 10
                    + CHINESE_NUMBER_MAP.getOrDefault(value.charAt(2), 0);
        }

        if (value.length() == 1) {
            return CHINESE_NUMBER_MAP.getOrDefault(value.charAt(0), 0);
        }

        return 0;
    }
}
