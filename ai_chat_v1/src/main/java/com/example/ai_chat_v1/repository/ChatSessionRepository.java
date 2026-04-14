package com.example.ai_chat_v1.repository;

import com.example.ai_chat_v1.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// 继承 JpaRepository<操作的实体类, 主键的类型>
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    // 👇 神奇的命名语法：Spring 会自动把它翻译成：
    // SELECT * FROM chat_session ORDER BY create_time DESC
    // 作用：查出所有历史会话，并且把最新的放在最上面（给左侧边栏用）
    List<ChatSession> findAllByOrderByCreateTimeDesc();
}