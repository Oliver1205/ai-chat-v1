package com.example.ai_chat_v1.repository;

import com.example.ai_chat_v1.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 👇 作用：根据 sessionId 查出这个会话里的所有聊天记录，按时间先后顺序排列（给右侧聊天框用）
    List<ChatMessage> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    // 👇 作用：当用户在侧边栏点击“删除会话”时，连带着把这个会话底下的所有聊天记录一起删掉
    void deleteBySessionId(String sessionId);
}