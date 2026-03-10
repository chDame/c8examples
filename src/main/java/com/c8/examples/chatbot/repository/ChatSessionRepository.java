package com.c8.examples.chatbot.repository;

import com.c8.examples.chatbot.model.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String> {
}
