package com.example.ai_chat_v1.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeTool {

    public String today() {
        LocalDate today = LocalDate.now();
        return "今天是" + today.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) + "。";
    }

    public String now() {
        LocalDateTime now = LocalDateTime.now();
        return "当前时间是" + now.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss")) + "。";
    }

    public String dayOfWeek() {
        LocalDate today = LocalDate.now();
        String weekday = switch (today.getDayOfWeek()) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
        return "今天是" + weekday + "。";
    }
}