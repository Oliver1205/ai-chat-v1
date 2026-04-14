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

    // 1. 当大模型需要回想之前的对话时，会自动调用这个方法
    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String sessionId = (String) memoryId;
        // 去数据库里按时间顺序查出这个会话的所有记录
        List<ChatMessage> dbMessages = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);

        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = new ArrayList<>();
        for (ChatMessage dbMsg : dbMessages) {
            // 神仙操作：把数据库里的 JSON 字符串“复活”成大模型认识的记忆对象！
            langchainMessages.add(ChatMessageDeserializer.messageFromJson(dbMsg.getContent()));
        }
        return langchainMessages;
    }

    // 2. 当你或者大模型说了新话，需要保存记忆时，会自动调用这个方法
    @Override
    @Transactional // 🚨 数据库有删改操作，必须加事务注解保证安全
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String sessionId = (String) memoryId;

        // 简单粗暴却稳妥的做法：每次记忆更新，先清空这个会话的旧记录
        messageRepository.deleteBySessionId(sessionId);

        // 然后把最新的完整记忆全量覆盖进去
        List<ChatMessage> dbMessages = new ArrayList<>();
        for (dev.langchain4j.data.message.ChatMessage msg : messages) {
            ChatMessage dbMsg = new ChatMessage();
            dbMsg.setSessionId(sessionId);
            dbMsg.setRole(msg.type().name());

            // 神仙操作：大模型不仅有普通对话，还有 Tool 调用（查天气等）。
            // 我们统一把它压缩成一段 JSON 存进 TEXT 字段里，它自己懂怎么解压！
            dbMsg.setContent(ChatMessageSerializer.messageToJson(msg));
            dbMsg.setCreateTime(LocalDateTime.now());
            dbMessages.add(dbMsg);
        }
        messageRepository.saveAll(dbMessages);
    }

    // 3. 当需要清空记忆时调用
    @Override
    @Transactional
    public void deleteMessages(Object memoryId) {
        messageRepository.deleteBySessionId((String) memoryId);
    }
}