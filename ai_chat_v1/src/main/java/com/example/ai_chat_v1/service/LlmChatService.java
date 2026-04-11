package com.example.ai_chat_v1.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class LlmChatService {

    private final StreamingChatModel chatModel;
    private final ChatMemoryManager memoryManager;
    private final KnowledgeBaseManager knowledgeBaseManager; // 引入知识库管理员

    public LlmChatService(StreamingChatModel chatModel, ChatMemoryManager memoryManager, KnowledgeBaseManager knowledgeBaseManager) {
        this.chatModel = chatModel;
        this.memoryManager = memoryManager;
        this.knowledgeBaseManager = knowledgeBaseManager;
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

        // --- RAG 第一步：Retrieval (检索) ---
        // 去知识库捞取和这个问题相关的“小抄”
        String referenceInfo = knowledgeBaseManager.search(userMessage);

        // 获取该用户的记忆档案
        ChatMemory chatMemory = memoryManager.getOrCreate(sessionId);

        // 【关键设计】：我们只把用户原始的、干净的问题存进记忆里，防止记忆被小抄污染
        chatMemory.add(UserMessage.from(userMessage));

        // 拿出所有的历史聊天记录准备发给大模型
        List<ChatMessage> messagesToSend = new ArrayList<>(chatMemory.messages());

        // --- RAG 第二步：Augmented (增强) ---
        // 如果捞到了小抄，我们就把最后一条（即本次提问）偷偷替换成“带小抄的终极版”
// --- RAG 第二步：Augmented (增强) ---
        if (!referenceInfo.isEmpty()) {
            messagesToSend.remove(messagesToSend.size() - 1);

            // 👇 优化后的 Prompt：兼顾知识库和通用知识
            String augmentedText = "下面是一些可能与我问题相关的【参考资料】。请优先基于参考资料回答。如果参考资料不足以回答，你可以利用自己的知识进行解答。\n\n" +
                    "【参考资料】\n" + referenceInfo + "\n\n" +
                    "【我的问题】\n" + userMessage;

            messagesToSend.add(UserMessage.from(augmentedText));
        }

        // --- RAG 第三步：Generation (生成) ---
        ChatRequest chatRequest = ChatRequest.builder().messages(messagesToSend).build();
        StringBuilder aiMessageBuilder = new StringBuilder();

        chatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                aiMessageBuilder.append(partialResponse);
                onToken.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 存入 AI 的最终回答
                chatMemory.add(AiMessage.from(aiMessageBuilder.toString()));
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }
}