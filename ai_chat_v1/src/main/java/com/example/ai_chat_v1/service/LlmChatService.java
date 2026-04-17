package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.tool.TimeTool;
import com.example.ai_chat_v1.tool.WeatherTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class LlmChatService {

    private final StreamingChatModel chatModel;
    private final ChatMemoryManager memoryManager;
    private final KnowledgeBaseManager knowledgeBaseManager;
    private final WeatherTool weatherTool;
    private final TimeTool timeTool;
    private final SessionTitleService sessionTitleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmChatService(StreamingChatModel chatModel,
                          ChatMemoryManager memoryManager,
                          KnowledgeBaseManager knowledgeBaseManager,
                          WeatherTool weatherTool,
                          TimeTool timeTool,
                          SessionTitleService sessionTitleService) {
        this.chatModel = chatModel;
        this.memoryManager = memoryManager;
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.weatherTool = weatherTool;
        this.timeTool = timeTool;
        this.sessionTitleService = sessionTitleService;
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

        sessionTitleService.ensureSessionExists(sessionId);
        sessionTitleService.touchSession(sessionId);

        ChatMemory chatMemory = memoryManager.getOrCreate(sessionId);

        // 先做时间类问题硬路由，彻底避免模型瞎猜日期
        if (isDateOrTimeQuestion(userMessage)) {
            handleTimeQuestion(chatMemory, userMessage, onToken, onComplete, onError, sessionId);
            return;
        }

        String referenceInfo = knowledgeBaseManager.search(userMessage);
        chatMemory.add(UserMessage.from(userMessage));

        List<ChatMessage> messagesToSend = new ArrayList<>(chatMemory.messages());

        if (!referenceInfo.isEmpty()) {
            messagesToSend.remove(messagesToSend.size() - 1);

            String augmentedText = "下面是一些【参考资料】。请仔细阅读并判断它们是否与我的问题真正相关。\n" +
                    "1. 如果相关，请结合资料进行回答。\n" +
                    "2. 🚨【极其重要】如果参考资料与我的问题【毫无关系】（例如我问天气、日期、时间或人物，资料却是公司规章或 Wi-Fi），请【完全忽略】这些资料，直接回答问题！\n" +
                    "3. 🚨【禁止事项】如果参考资料无关，【绝对不要】在回答里提“参考资料无关”“根据资料”“资料中没有提到”等说明，直接自然回答。\n\n" +
                    "【参考资料】\n" + referenceInfo + "\n\n" +
                    "【我的问题】\n" + userMessage;

            messagesToSend.add(UserMessage.from(augmentedText));
        }

        executeAgentLoop(sessionId, chatMemory, messagesToSend, onToken, onComplete, onError);
    }

    private void handleTimeQuestion(ChatMemory chatMemory,
                                    String userMessage,
                                    Consumer<String> onToken,
                                    Runnable onComplete,
                                    Consumer<Throwable> onError,
                                    String sessionId) {
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

            CompletableFuture.runAsync(() -> {
                try {
                    sessionTitleService.tryAutoGenerateTitle(sessionId);
                } catch (Exception ignored) {
                }
            });

            onComplete.run();
        } catch (Exception e) {
            onError.accept(e);
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

    private void executeAgentLoop(String sessionId,
                                  ChatMemory chatMemory,
                                  List<ChatMessage> messagesToSend,
                                  Consumer<String> onToken,
                                  Runnable onComplete,
                                  Consumer<Throwable> onError) {

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messagesToSend)
                .toolSpecifications(ToolSpecifications.toolSpecificationsFrom(weatherTool))
                .build();

        chatModel.chat(chatRequest, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                onToken.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage aiMessage = completeResponse.aiMessage();
                chatMemory.add(aiMessage);

                if (aiMessage.hasToolExecutionRequests()) {
                    for (ToolExecutionRequest toolReq : aiMessage.toolExecutionRequests()) {
                        try {
                            ToolExecutionResultMessage toolMessage = executeTool(toolReq);
                            chatMemory.add(toolMessage);
                            messagesToSend.add(aiMessage);
                            messagesToSend.add(toolMessage);
                        } catch (Exception e) {
                            onError.accept(e);
                            return;
                        }
                    }

                    executeAgentLoop(sessionId, chatMemory, messagesToSend, onToken, onComplete, onError);
                    return;
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        sessionTitleService.tryAutoGenerateTitle(sessionId);
                    } catch (Exception ignored) {
                    }
                });

                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }

    private ToolExecutionResultMessage executeTool(ToolExecutionRequest toolReq) throws Exception {
        String toolName = toolReq.name();

        if ("getWeather".equals(toolName)) {
            JsonNode args = objectMapper.readTree(toolReq.arguments());
            String city = args.has("city") ? args.get("city").asText() : "";
            String toolResult = weatherTool.getWeather(city);
            return ToolExecutionResultMessage.from(toolReq, toolResult);
        }

        throw new IllegalStateException("暂不支持的工具调用：" + toolName);
    }
}