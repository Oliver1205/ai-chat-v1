package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.repository.ChatMessageRepository;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DbChatMemoryStore implements ChatMemoryStore {

    private final ChatMessageRepository messageRepository;

    public DbChatMemoryStore(ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String sessionId = (String) memoryId;
        List<ChatMessage> dbMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);

        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = new ArrayList<>();
        for (ChatMessage dbMsg : dbMessages) {
            langchainMessages.add(ChatMessageDeserializer.messageFromJson(dbMsg.getContent()));
        }
        return langchainMessages;
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String sessionId = (String) memoryId;

        messageRepository.deleteBySessionId(sessionId);

        List<ChatMessage> dbMessages = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (dev.langchain4j.data.message.ChatMessage msg : messages) {
            ChatMessage dbMsg = new ChatMessage();
            dbMsg.setSessionId(sessionId);

            // 工具消息类型名可能很长，这里仍然保留原始类型，但数据库字段长度已经放大到 100
            String role = msg.type().name();
            if (role.length() > 100) {
                role = role.substring(0, 100);
            }
            dbMsg.setRole(role);

            dbMsg.setContent(ChatMessageSerializer.messageToJson(msg));
            dbMsg.setCreateTime(now);

            dbMessages.add(dbMsg);
        }

        messageRepository.saveAll(dbMessages);
    }

    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        messageRepository.deleteBySessionId((String) memoryId);
    }
}