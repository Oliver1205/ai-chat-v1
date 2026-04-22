package com.example.ai_chat_v1.agent.studycoach;

import com.example.ai_chat_v1.service.ChatSessionContextService;
import com.example.ai_chat_v1.service.SessionAutoTitleTrigger;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class StudyCoachAgentService {

    private static final int STREAM_CHUNK_SIZE = 48;

    private final ChatSessionContextService chatSessionContextService;
    private final StudyCoachReactOrchestrator reactOrchestrator;
    private final SessionAutoTitleTrigger sessionAutoTitleTrigger;

    public StudyCoachAgentService(ChatSessionContextService chatSessionContextService,
                                  StudyCoachReactOrchestrator reactOrchestrator,
                                  SessionAutoTitleTrigger sessionAutoTitleTrigger) {
        this.chatSessionContextService = chatSessionContextService;
        this.reactOrchestrator = reactOrchestrator;
        this.sessionAutoTitleTrigger = sessionAutoTitleTrigger;
    }

    public void streamChat(String sessionId,
                           String userMessage,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Throwable> onError) {
        if (userMessage == null || userMessage.isBlank()) {
            onError.accept(new IllegalArgumentException("消息不能为空"));
            return;
        }

        try {
            ChatMemory chatMemory = chatSessionContextService.prepare(sessionId);
            String finalAnswer = reactOrchestrator.execute(sessionId, userMessage);

            chatMemory.add(UserMessage.from(userMessage));
            chatMemory.add(AiMessage.from(finalAnswer));

            streamInChunks(finalAnswer, onToken);
            sessionAutoTitleTrigger.triggerAsync(sessionId);
            onComplete.run();
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    private void streamInChunks(String content, Consumer<String> onToken) {
        if (content == null || content.isBlank()) {
            return;
        }

        for (int start = 0; start < content.length(); start += STREAM_CHUNK_SIZE) {
            int end = Math.min(content.length(), start + STREAM_CHUNK_SIZE);
            onToken.accept(content.substring(start, end));
        }
    }
}
