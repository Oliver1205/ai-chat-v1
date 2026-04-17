package com.example.ai_chat_v1.repository;

import com.example.ai_chat_v1.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findAllByOrderByLastActiveTimeDescCreateTimeDesc();
}