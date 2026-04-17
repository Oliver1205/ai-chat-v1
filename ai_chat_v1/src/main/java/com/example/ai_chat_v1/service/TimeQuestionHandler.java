package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.tool.TimeTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class TimeQuestionHandler {

    private final TimeTool timeTool;
    private final SessionAutoTitleTrigger sessionAutoTitleTrigger;

    public TimeQuestionHandler(TimeTool timeTool,
                               SessionAutoTitleTrigger sessionAutoTitleTrigger) {
        this.timeTool = timeTool;
        this.sessionAutoTitleTrigger = sessionAutoTitleTrigger;
    }

    public boolean tryHandle(String sessionId,
                             ChatMemory chatMemory,
                             String userMessage,
                             Consumer<String> onToken,
                             Runnable onComplete,
                             Consumer<Throwable> onError) {
        if (!isDateOrTimeQuestion(userMessage)) {
            return false;
        }

        try {
            chatMemory.add(UserMessage.from(userMessage));

            String answer;
            if (containsAny(userMessage, "几点", "时间", "现在几时", "现在几点", "当前时间")) {
                answer = timeTool.now();
            } else if (containsAny(userMessage, "星期几", "周几", "礼拜几")) {
                answer = timeTool.dayOfWeek();
            } else {
                answer = timeTool.today();
            }

            chatMemory.add(AiMessage.from(answer));
            onToken.accept(answer);

            sessionAutoTitleTrigger.triggerAsync(sessionId);

            onComplete.run();
            return true;
        } catch (Exception e) {
            onError.accept(e);
            return true;
        }
    }

    private boolean isDateOrTimeQuestion(String text) {
        return containsAny(text,
                "今天几号", "今天是几号", "今天几月几号", "今天多少号",
                "今天日期", "当前日期", "今日日期",
                "今天星期几", "今天周几", "今天礼拜几",
                "现在几点", "现在几时", "当前时间", "现在时间");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}