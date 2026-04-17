package com.example.ai_chat_v1.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatMessagePreparer {

    private final KnowledgeBaseManager knowledgeBaseManager;
    private final ReferencePromptBuilder referencePromptBuilder;

    public ChatMessagePreparer(KnowledgeBaseManager knowledgeBaseManager,
                               ReferencePromptBuilder referencePromptBuilder) {
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.referencePromptBuilder = referencePromptBuilder;
    }

    public List<ChatMessage> prepare(ChatMemory chatMemory, String userMessage) {
        String referenceInfo = knowledgeBaseManager.search(userMessage);

        chatMemory.add(UserMessage.from(userMessage));

        List<ChatMessage> messagesToSend = new ArrayList<>(chatMemory.messages());

        if (referenceInfo != null && !referenceInfo.isBlank() && !messagesToSend.isEmpty()) {
            messagesToSend.remove(messagesToSend.size() - 1);

            String augmentedText = referencePromptBuilder.build(userMessage, referenceInfo);
            messagesToSend.add(UserMessage.from(augmentedText));
        }

        return messagesToSend;
    }
}