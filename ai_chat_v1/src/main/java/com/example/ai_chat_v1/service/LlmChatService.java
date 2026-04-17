package com.example.ai_chat_v1.service;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class LlmChatService {

    private final StreamingChatModel chatModel;
    private final ChatSessionContextService chatSessionContextService;
    private final TimeQuestionHandler timeQuestionHandler;
    private final ChatToolManager chatToolManager;
    private final ChatMessagePreparer chatMessagePreparer;
    private final SessionAutoTitleTrigger sessionAutoTitleTrigger;

    public LlmChatService(StreamingChatModel chatModel,
                          ChatSessionContextService chatSessionContextService,
                          TimeQuestionHandler timeQuestionHandler,
                          ChatToolManager chatToolManager,
                          ChatMessagePreparer chatMessagePreparer,
                          SessionAutoTitleTrigger sessionAutoTitleTrigger) {
        this.chatModel = chatModel;
        this.chatSessionContextService = chatSessionContextService;
        this.timeQuestionHandler = timeQuestionHandler;
        this.chatToolManager = chatToolManager;
        this.chatMessagePreparer = chatMessagePreparer;
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

        ChatMemory chatMemory = chatSessionContextService.prepare(sessionId);

        if (timeQuestionHandler.tryHandle(sessionId, chatMemory, userMessage, onToken, onComplete, onError)) {
            return;
        }

        List<ChatMessage> messagesToSend = chatMessagePreparer.prepare(chatMemory, userMessage);

        executeAgentLoop(sessionId, chatMemory, messagesToSend, onToken, onComplete, onError);
    }

    private void executeAgentLoop(String sessionId,
                                  ChatMemory chatMemory,
                                  List<ChatMessage> messagesToSend,
                                  Consumer<String> onToken,
                                  Runnable onComplete,
                                  Consumer<Throwable> onError) {

        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .messages(messagesToSend);

        ChatRequest chatRequest = chatToolManager
                .applyToolSpecifications(requestBuilder)
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
                            ToolExecutionResultMessage toolMessage = chatToolManager.execute(toolReq);
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

                sessionAutoTitleTrigger.triggerAsync(sessionId);
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }
}