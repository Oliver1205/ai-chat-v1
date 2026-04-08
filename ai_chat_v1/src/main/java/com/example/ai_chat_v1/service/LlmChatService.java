package com.example.ai_chat_v1.service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel; // 注意：类名变短了！
import dev.langchain4j.model.chat.request.ChatRequest; // 新引入了 Request 包装类
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class LlmChatService {

    // 注入最新版的大模型接口
    private final StreamingChatModel chatModel;

    public LlmChatService(StreamingChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void streamChat(String userMessage,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Throwable> onError) {

        if (userMessage == null || userMessage.isBlank()) {
            onError.accept(new IllegalArgumentException("消息不能为空"));
            return;
        }

        // 新版的规范：用 ChatRequest 包装消息
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from("你是一个耐心、清晰、友好的 AI 助手。回答时尽量条理化，优先使用中文。"),
                        UserMessage.from(userMessage)
                ))
                .build();

        // 呼叫大模型，方法名和回调监听器都变了
        chatModel.chat(chatRequest, new StreamingChatResponseHandler() {

            // 吐出一个字
            @Override
            public void onPartialResponse(String partialResponse) {
                onToken.accept(partialResponse);
            }

            // 全部说完
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                onComplete.run();
            }

            // 发生报错
            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }
}